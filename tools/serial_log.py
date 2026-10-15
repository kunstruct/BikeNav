#!/usr/bin/env python3
"""Captures the board's serial log for N seconds; optional commands are sent after a delay.
   python tools/serial_log.py 20 [--reset] [--send "5:demo"]"""
import argparse
import sys
import time
import serial
from serial.tools import list_ports


def timed_command(value):
    try:
        delay, command = value.split(":", 1)
        delay = float(delay)
        if delay < 0 or not command:
            raise ValueError
        return delay, command
    except ValueError as exc:
        raise argparse.ArgumentTypeError("use SECONDS:COMMAND, e.g. 3:touchtest") from exc


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("seconds", nargs="?", type=float, default=15)
    parser.add_argument("--port", help="serial device, e.g. /dev/ttyACM0 or COM3")
    parser.add_argument("--reset", action="store_true")
    parser.add_argument("--send", action="append", type=timed_command, default=[])
    args = parser.parse_args()
    if not args.port:
        ports = [p.device for p in list_ports.comports() if p.vid == 0x303A]
        if len(ports) != 1:
            parser.error("use --port; expected exactly one connected Espressif USB device")
        args.port = ports[0]
    with serial.Serial(args.port, 115200, timeout=0.2) as conn:
        if args.reset:
            conn.dtr = False; conn.rts = True
            time.sleep(0.1)
            conn.rts = False
        start = time.monotonic()
        sent = set()
        while time.monotonic() - start < args.seconds:
            for i, (delay, command) in enumerate(args.send):
                if i not in sent and time.monotonic() - start >= delay:
                    conn.write((command + "\n").encode())
                    sent.add(i)
                    print(f">>> {command}", flush=True)
            data = conn.read(4096)
            if data:
                sys.stdout.write(data.decode(errors="replace"))
                sys.stdout.flush()


if __name__ == "__main__":
    main()
