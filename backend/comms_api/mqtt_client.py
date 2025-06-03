import json
import paho.mqtt.client as mqtt
from django.conf import settings

MQTT_HOST = settings.HOST
MQTT_PORT = 1883


def send_notification(to_user_id, payload):
    client = mqtt.Client()
    client.connect(MQTT_HOST, MQTT_PORT, 60)

    topic = f"user/{to_user_id}"
    message = json.dumps(payload)

    client.publish(topic, message)
    client.disconnect()
