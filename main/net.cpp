// Saved connection modes; default is board AP only (no phone hotspot required):
//  - Access point "MiniNav": the phone joins it; the address DHCP hands the phone is where its
//    Android Auto headunit server listens. Some phones (Samsung) then lose mobile internet.
//  - Phone hotspot: the board joins the phone's hotspot "MiniNavHotspot" as a station and talks
//    to the headunit server at the hotspot's gateway address. The phone keeps its internet.
// Auto mode enables both; the phone hotspot wins when both are up.
// Helper WiFi hosts the same AP but waits for the Android helper on TCP 5278.
#include <string.h>
#include <atomic>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_netif.h"
#include "esp_mac.h"
#include "esp_timer.h"
#include "lwip/ip4_addr.h"
#include "app.h"
#include "wifi_policy.h"

static const char *TAG = "net";
static portMUX_TYPE s_lock = portMUX_INITIALIZER_UNLOCKED;
static wifi_endpoint s_endpoint;
static std::atomic<bool> s_sta_connected{false};
static std::atomic<int> s_ap_clients{0};
static std::atomic<uint32_t> s_sta_disconnects{0};
static std::atomic<unsigned> s_retry_count{0};
static std::atomic<int> s_disconnect_reason{0};
static std::atomic<bool> s_started{false};
static std::atomic<TaskHandle_t> s_hotspot_task{nullptr};
static uint8_t s_ap_mac[6]; // owned by the ESP event-loop task

static void show_ip(void) {
    char ip[16] = "";
    net_phone_ip(ip, sizeof ip);
    strlcpy(g_app.phone_ip, ip, sizeof g_app.phone_ip);
}

bool net_phone_endpoint(char *out, int len, uint32_t *generation) {
    if (!out || len <= 0) return false;
    portENTER_CRITICAL(&s_lock);
    strlcpy(out, s_endpoint.current, len);
    if (generation) *generation = s_endpoint.generation;
    bool ok = s_endpoint.current[0] != 0;
    portEXIT_CRITICAL(&s_lock);
    return ok;
}

bool net_phone_ip(char *out, int len) { return net_phone_endpoint(out, len, nullptr); }

bool net_link_valid(uint32_t generation) {
    portENTER_CRITICAL(&s_lock);
    bool ok = s_endpoint.current[0] && s_endpoint.generation == generation;
    portEXIT_CRITICAL(&s_lock);
    return ok;
}

void net_set_streaming(bool active) {
    if (!s_started || g_app.wifi_mode != 1) return;
    wifi_ps_type_t ps = WIFI_PS_NONE;
#if CONFIG_AAM_STA_IDLE_POWER_SAVE
    if (!active) ps = WIFI_PS_MIN_MODEM;
#endif
    esp_err_t err = esp_wifi_set_ps(ps);
    if (err != ESP_OK) ESP_LOGW(TAG, "power-save update failed: %s", esp_err_to_name(err));
}

void net_diagnostics(void) {
    char ip[16]; uint32_t generation;
    net_phone_endpoint(ip, sizeof ip, &generation);
    uint8_t channel = 0; wifi_second_chan_t second;
    esp_wifi_get_channel(&channel, &second);
    wifi_ps_type_t ps = WIFI_PS_NONE;
    esp_wifi_get_ps(&ps);
    ESP_LOGI(TAG, "mode=%d channel=%u peer=%s epoch=%lu AP-clients=%d retries=%u last-reason=%d idle-sleep=%s",
             g_app.wifi_mode, channel, ip[0] ? ip : "none", (unsigned long)generation,
             s_ap_clients.load(), s_retry_count.load(), s_disconnect_reason.load(),
             ps == WIFI_PS_NONE ? "off" : "on");
    if (s_sta_connected) {
        wifi_ap_record_t ap = {};
        if (esp_wifi_sta_get_ap_info(&ap) == ESP_OK) ESP_LOGI(TAG, "phone hotspot RSSI=%d dBm", ap.rssi);
    } else if (s_ap_clients > 0) {
        wifi_sta_list_t stations = {};
        if (esp_wifi_ap_get_sta_list(&stations) == ESP_OK && stations.num)
            ESP_LOGI(TAG, "phone RSSI=%d dBm", stations.sta[0].rssi);
    }
}

static void on_event(void *, esp_event_base_t base, int32_t id, void *data) {
    if (base == IP_EVENT && id == IP_EVENT_AP_STAIPASSIGNED) {
        auto *e = (ip_event_ap_staipassigned_t *)data;
        if (s_ap_clients == 0 || memcmp(e->mac, s_ap_mac, sizeof s_ap_mac)) return;
        char ip[16];
        snprintf(ip, sizeof ip, IPSTR, IP2STR(&e->ip));
        portENTER_CRITICAL(&s_lock);
        s_endpoint.set_ap(ip);
        portEXIT_CRITICAL(&s_lock);
        show_ip();
        ESP_LOGI(TAG, "phone joined, IP %s", ip);
        if (g_app.stage < STAGE_PHONE) app_set_stage(STAGE_PHONE);
        else display_request_status();
    } else if (base == WIFI_EVENT && id == WIFI_EVENT_AP_STADISCONNECTED) {
        auto *e = (wifi_event_ap_stadisconnected_t *)data;
        if (memcmp(e->mac, s_ap_mac, sizeof s_ap_mac)) return;
        s_ap_clients = 0; // AP admits exactly one phone
        s_disconnect_reason = e->reason;
        ESP_LOGI(TAG, "phone left board WiFi, reason=%u", e->reason);
        portENTER_CRITICAL(&s_lock);
        s_endpoint.set_ap("");
        portEXIT_CRITICAL(&s_lock);
        show_ip();
        char phone[16];
        if (!net_phone_ip(phone, sizeof phone) && g_app.stage >= STAGE_PHONE && g_app.stage < STAGE_TCP) app_set_stage(STAGE_AP);
        else display_request_status();
    } else if (base == WIFI_EVENT && id == WIFI_EVENT_AP_STACONNECTED) {
        auto *e = (wifi_event_ap_staconnected_t *)data;
        ESP_LOGI(TAG, "station " MACSTR " associated", MAC2STR(e->mac));
        memcpy(s_ap_mac, e->mac, sizeof s_ap_mac);
        s_ap_clients = 1;
        // A reassociation may reuse an address. Invalidate the old TCP session
        // before DHCP assigns this association's address.
        portENTER_CRITICAL(&s_lock);
        s_endpoint.set_ap("");
        portEXIT_CRITICAL(&s_lock);
        show_ip();
    } else if (base == IP_EVENT && id == IP_EVENT_STA_GOT_IP) {
        auto *e = (ip_event_got_ip_t *)data;
        char ip[16];
        snprintf(ip, sizeof ip, IPSTR, IP2STR(&e->ip_info.gw));
        portENTER_CRITICAL(&s_lock);
        s_endpoint.set_hotspot(ip);
        portEXIT_CRITICAL(&s_lock);
        show_ip();
        ESP_LOGI(TAG, "joined phone hotspot \"%s\", phone at %s", CONFIG_AAM_HOTSPOT_SSID, ip);
        if (g_app.stage < STAGE_PHONE) app_set_stage(STAGE_PHONE);
        else display_request_status();
    } else if (base == IP_EVENT && id == IP_EVENT_STA_LOST_IP) {
        portENTER_CRITICAL(&s_lock);
        s_endpoint.set_hotspot("");
        portEXIT_CRITICAL(&s_lock);
        show_ip();
        display_request_status();
    } else if (base == WIFI_EVENT && id == WIFI_EVENT_STA_CONNECTED) {
        s_sta_connected = true;
    } else if (base == WIFI_EVENT && id == WIFI_EVENT_STA_DISCONNECTED) {
        s_sta_connected = false;
        auto *e = (wifi_event_sta_disconnected_t *)data;
        s_disconnect_reason = e->reason;
        ESP_LOGI(TAG, "phone hotspot disconnected, reason=%u", e->reason);
        portENTER_CRITICAL(&s_lock);
        bool had = s_endpoint.hotspot[0] != 0;
        s_endpoint.set_hotspot("");
        portEXIT_CRITICAL(&s_lock);
        ++s_sta_disconnects;
        if (had) {
            ESP_LOGI(TAG, "left phone hotspot");
            show_ip();
            display_request_status();
        }
    }
    TaskHandle_t task = s_hotspot_task.load();
    if (task) xTaskNotifyGive(task);
}

// The worker owns retry policy. No scan while a phone uses the board AP,
// and no repeated connect calls while an association/DHCP attempt is in flight.
static void hotspot_task(void *) {
    wifi_retry retry;
    uint32_t disconnects = 0;
    bool attempting = false;
    int64_t attempt_started = 0;
    for (;;) {
        int64_t now = esp_timer_get_time() / 1000;
        uint32_t latest = s_sta_disconnects.load();
        if (latest != disconnects) {
            disconnects = latest;
            attempting = false;
            retry.failed(now);
            s_retry_count = retry.failures;
            ESP_LOGI(TAG, "hotspot retry in %lld ms", (long long)(retry.next_ms - now));
        }
        portENTER_CRITICAL(&s_lock);
        bool has_ip = s_endpoint.hotspot[0] != 0;
        portEXIT_CRITICAL(&s_lock);
        if (has_ip) {
            attempting = false;
            retry.connected();
            s_retry_count = 0;
        } else if (!attempting && s_sta_connected) {
            attempting = true;
            attempt_started = now;
        } else if (attempting && s_ap_clients > 0 && !s_sta_connected) {
            // A phone joined our AP during a scan. Cancel the station attempt
            // rather than repeatedly taking the radio off its map channel.
            esp_wifi_disconnect();
            attempting = false;
            retry.failed(now);
            s_retry_count = retry.failures;
        } else if (attempting && now - attempt_started >= 15000) {
            // Association without DHCP must not suppress retries forever.
            ESP_LOGW(TAG, "hotspot setup timed out; reconnecting");
            esp_err_t err = esp_wifi_disconnect();
            attempt_started = now;
            if (err != ESP_OK) { attempting = false; retry.failed(now); s_retry_count = retry.failures; }
        } else if (!attempting && !s_sta_connected && retry.due(now, s_ap_clients > 0)) {
            esp_err_t err = esp_wifi_connect();
            if (err == ESP_OK) { attempting = true; attempt_started = now; }
            else { retry.failed(now); s_retry_count = retry.failures; }
        }
        // Sleep until a link event or the next actual retry/deadline. A working
        // hotspot or a phone on our AP causes no periodic management wakeups.
        TickType_t wait = portMAX_DELAY;
        if (!has_ip && attempting) {
            int64_t remaining = 15000 - (esp_timer_get_time() / 1000 - attempt_started);
            wait = pdMS_TO_TICKS(remaining > 0 ? remaining : 1);
        } else if (!has_ip && s_ap_clients == 0) {
            int64_t remaining = retry.next_ms - esp_timer_get_time() / 1000;
            wait = pdMS_TO_TICKS(remaining > 0 ? remaining : 1);
        }
        if (wait == 0) wait = 1;
        ulTaskNotifyTake(pdTRUE, wait);
    }
}

void net_start(void) {
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    wifi_mode_t mode = (g_app.wifi_mode == 0 || g_app.wifi_mode == 3) ? WIFI_MODE_AP : g_app.wifi_mode == 1 ? WIFI_MODE_STA : WIFI_MODE_APSTA;
    esp_netif_t *ap_netif = mode != WIFI_MODE_STA ? esp_netif_create_default_wifi_ap() : nullptr;
    if (mode != WIFI_MODE_AP) configASSERT(esp_netif_create_default_wifi_sta());
    // Hand out an address only: no gateway, no DNS. With a gateway the phone sends its
    // internet traffic (and DNS) to the board, which has no internet, and Maps breaks with
    // DNS_PROBE errors. Omitting these leaves cellular default routing possible,
    // though Android/phone policy still decides whether to keep both networks.
    if (ap_netif) {
        uint8_t offer = 0;
        ESP_ERROR_CHECK(esp_netif_dhcps_stop(ap_netif));
        esp_netif_ip_info_t ipi = {};
        ESP_ERROR_CHECK(esp_netif_get_ip_info(ap_netif, &ipi));
        ipi.gw.addr = 0;  // belt and braces: nothing to offer as a router
        ESP_ERROR_CHECK(esp_netif_set_ip_info(ap_netif, &ipi));
        ESP_ERROR_CHECK(esp_netif_dhcps_option(ap_netif, ESP_NETIF_OP_SET, ESP_NETIF_ROUTER_SOLICITATION_ADDRESS, &offer, sizeof offer));
        ESP_ERROR_CHECK(esp_netif_dhcps_option(ap_netif, ESP_NETIF_OP_SET, ESP_NETIF_DOMAIN_NAME_SERVER, &offer, sizeof offer));
        ESP_ERROR_CHECK(esp_netif_dhcps_start(ap_netif));
    }
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_AP_STAIPASSIGNED, on_event, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, on_event, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_LOST_IP, on_event, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, on_event, NULL));

    wifi_config_t ap = {};
    strlcpy((char *)ap.ap.ssid, CONFIG_AAM_AP_SSID, sizeof ap.ap.ssid);
    ap.ap.ssid_len = strlen(CONFIG_AAM_AP_SSID);
    strlcpy((char *)ap.ap.password, CONFIG_AAM_AP_PASSWORD, sizeof ap.ap.password);
    ap.ap.channel = CONFIG_AAM_AP_CHANNEL;
    ap.ap.beacon_interval = 100;
    ap.ap.dtim_period = CONFIG_AAM_AP_DTIM;
    ap.ap.max_connection = 1; // exactly one phone; its DHCP lease identifies the server
    ap.ap.authmode = WIFI_AUTH_WPA2_PSK;
    ap.ap.pmf_cfg.required = false;
    wifi_config_t sta = {};
    strlcpy((char *)sta.sta.ssid, CONFIG_AAM_HOTSPOT_SSID, sizeof sta.sta.ssid);
    strlcpy((char *)sta.sta.password, CONFIG_AAM_HOTSPOT_PASSWORD, sizeof sta.sta.password);
    sta.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;
    sta.sta.scan_method = WIFI_FAST_SCAN;
    sta.sta.failure_retry_cnt = 0; // retry/backoff is owned by hotspot_task
    ESP_ERROR_CHECK(esp_wifi_set_mode(mode));
    if (mode != WIFI_MODE_STA) {
        ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_AP, &ap));
        ESP_ERROR_CHECK(esp_wifi_set_bandwidth(WIFI_IF_AP, WIFI_BW_HT20));
    }
    if (mode != WIFI_MODE_AP) ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &sta));
    ESP_ERROR_CHECK(esp_wifi_start());
    ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));
    s_started = true;
    net_set_streaming(false);
    ESP_LOGI(TAG, "WiFi mode: %s", g_app.wifi_mode == 3 ? "Helper WiFi (Android relay required)" : mode == WIFI_MODE_AP ? "Board WiFi (phone hotspot not required)" :
             mode == WIFI_MODE_STA ? "Phone hotspot" : "Automatic AP + phone hotspot");
    app_set_stage(STAGE_AP);
    if (mode != WIFI_MODE_AP) {
        TaskHandle_t task = nullptr;
        configASSERT(xTaskCreatePinnedToCore(hotspot_task, "hotspot", 3072, nullptr, 3, &task, 0) == pdPASS);
        s_hotspot_task = task;
        xTaskNotifyGive(task); // pick up events that arrived before the handle was published
    }
}
