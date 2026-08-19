import socket
import ssl
import sys

project_ref = "zrssiiwgditrrojxiako"
password = "Patrol@1234.0"

regions = [
    "ap-south-1",
    "us-east-1",
    "us-west-1",
    "eu-central-1",
    "ap-southeast-1",
    "sa-east-1"
]

print(f"Testing Supabase Pooler Endpoints for project {project_ref}...")

for region in regions:
    host = f"aws-0-{region}.pooler.supabase.com"
    for port in [6543, 5432]:
        try:
            sock = socket.create_connection((host, port), timeout=3)
            print(f"[OK] Connected to {host}:{port}")
            sock.close()
        except Exception as e:
            print(f"[FAIL] {host}:{port} -> {e}")
