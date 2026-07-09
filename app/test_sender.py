import time
import urllib.request
import urllib.error
import json

webhook_url = "https://easywallet-v-3--edm1175qq.replit.app/api/v1/sms/callback"
headers = {
    "X-SMS-Token": "fd49e732c5f5ed78fe5fe38b5f8ac8c2",
    "Content-Type": "application/json",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
}

test_messages = [
    {
        "sender": "K-Bank",
        "message": "คุณได้รับโอนเงินจาก นายวิทยา จำนวน 1,500.00 บาท วันที่ 09/07/2026 13:40",
    },
    {
        "sender": "SCB",
        "message": "เงินเข้า บัญชี x1234 ยอด 2,350.00 บาท จาก SCB EASY App",
    },
    {
        "sender": "TrueMoney",
        "message": "คุณได้รับเงิน 500.00 บาท จาก บจก. เทสการค้า",
    },
    {
        "sender": "Krungthai",
        "message": "เงินเข้าบัญชี Krungthai NEXT จำนวน 10,000.00 บาท",
    },
    {
        "sender": "K PLUS",
        "message": "ได้รับโอนเงินจำนวน 4,200.00 บาท จาก น.ส. รัตนา",
    },
    {
        "sender": "SCB",
        "message": "ได้รับโอนเงินจำนวน 8,500.00 บาท จาก SCB EASY 09/07 13:45",
    },
    {
        "sender": "KBank",
        "message": "ยอดเงินเข้า 350.00 บาท จาก นายประดิษฐ์",
    },
    {
        "sender": "Krungsri",
        "message": "เงินเข้าบัญชี x9876 จำนวน 1,200.00 บาท",
    },
    {
        "sender": "TrueMoney",
        "message": "เติมเงินสำเร็จ 150.00 บาท ผ่านโมบายแบงก์กิ้ง",
    },
    {
        "sender": "SCB",
        "message": "เงินเข้าบัญชี x5678 จำนวน 600.00 บาท จาก บัญชีต่างธนาคาร",
    }
]

print("Starting to send 10 test messages to the webhook...")
print(f"Webhook URL: {webhook_url}")
print("-" * 50)

for idx, item in enumerate(test_messages, 1):
    payload = {
        "sender": item["sender"],
        "message": item["message"],
        "timestamp": int(time.time() * 1000),
        "device": "Google Pixel 8 (Simulated Test Forwarder)"
    }
    
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(webhook_url, data=data, headers=headers, method="POST")
    
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            status = response.status
            body = response.read().decode("utf-8")
            print(f"[{idx}/10] Sent from {item['sender']}: '{item['message'][:30]}...'")
            print(f"      Response Status: {status}")
            print(f"      Response Body: {body[:120]}")
    except urllib.error.HTTPError as e:
        print(f"[{idx}/10] HTTP Error: {e.code}")
        try:
            error_body = e.read().decode("utf-8")
            print(f"      Error Body: {error_body[:120]}")
        except:
            pass
    except Exception as e:
        print(f"[{idx}/10] Error sending message: {e}")
        
    time.sleep(1)

print("-" * 50)
print("Finished sending all 10 test messages!")
