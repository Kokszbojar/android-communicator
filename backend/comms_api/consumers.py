import json
import base64
from channels.generic.websocket import AsyncWebsocketConsumer
from asgiref.sync import sync_to_async
from django.core.files.base import ContentFile
from django.contrib.auth.models import AnonymousUser, User
from comms_api.models import Message, Call
from comms_api.serializers import MessageSerializer


class ChatConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        user = self.scope["user"]
        if user == AnonymousUser():
            await self.close()
        else:
            self.user = user
            self.room_name = f"user_{user.id}"
            await self.channel_layer.group_add(self.room_name, self.channel_name)
            await self.accept()

    async def disconnect(self, close_code):
        if hasattr(self, "room_name"):
            await self.channel_layer.group_discard(self.room_name, self.channel_name)
        else:
            await self.close()

    async def receive(self, text_data):
        data = json.loads(text_data)
        action = data.get("action")

        if action == "send_message":
            await self.handle_send_message(data)
        elif action == "webrtc_offer":
            await self.handle_webrtc_offer(data)
        elif action == "webrtc_answer":
            await self.handle_webrtc_answer(data)
        elif action == "webrtc_ice_candidate":
            await self.handle_webrtc_ice_candidate(data)
        elif action == "call_user":
            await self.handle_call_user(data)
        elif action == "answer_call":
            await self.handle_answer_call(data)

    async def handle_send_message(self, data):
        from_user = self.user
        to_user_id = data["to"]
        content = data["message"]
        to_user = await sync_to_async(User.objects.get)(id=to_user_id)
        file = None
        file_type = ""
        if "file" in data.keys():
            file_data = data["file"]
            file_type = data["file_type"]
            format, file_str = file_data.split(";base64,")
            ext = format.split("/")[-1]
            decoded_file = ContentFile(base64.b64decode(file_str), name=f"message_{from_user.id}_{to_user_id}.{ext}")
            file = decoded_file

        message = await sync_to_async(Message.objects.create)(
            sender=from_user,
            recipient=to_user,
            content=content,
            file=file,
            file_type=file_type or ""
        )

        await self.channel_layer.group_send(
            f"user_{to_user_id}",
            {
                "type": "chat.message",
                "message": {
                    "sender_name": from_user.username,
                    "content": content,
                    "file_url": f"http://192.168.0.130:8000{message.file.url}" if message.file else None,
                    "file_type": file_type,
                    "timestamp": "Teraz",
                },
            }
        )

    async def handle_webrtc_offer(self, data):
        to_user_id = data["to"]
        sdp = data["sdp"]

        await self.channel_layer.group_send(
            f"user_{to_user_id}",
            {
                "type": "webrtc.offer",
                "offer": {
                    "from": self.user.id,
                    "sdp": sdp,
                },
            }
        )

    async def handle_webrtc_answer(self, data):
        to_user_id = data["to"]
        sdp = data["sdp"]

        await self.channel_layer.group_send(
            f"user_{to_user_id}",
            {
                "type": "webrtc.answer",
                "answer": {
                    "from": self.user.id,
                    "sdp": sdp,
                },
            }
        )

    async def handle_webrtc_ice_candidate(self, data):
        to_user_id = data["to"]
        candidate = data["candidate"]

        await self.channel_layer.group_send(
            f"user_{to_user_id}",
            {
                "type": "webrtc.ice_candidate",
                "ice": {
                    "from": self.user.id,
                    "candidate": candidate,
                },
            }
        )

    async def handle_call_user(self, data):
        callee_id = data["to"]
        callee = await sync_to_async(User.objects.get)(id=callee_id)

        call = await sync_to_async(Call.objects.create)(
            caller=self.user,
            callee=callee,
        )

        await self.channel_layer.group_send(
            f"user_{callee_id}",
            {
                "type": "call.incoming",
                "call": {
                    "caller": self.user.id,
                    "call_id": call.id,
                },
            }
        )

    async def handle_answer_call(self, data):
        call_id = data["call_id"]
        accepted = data["accepted"]

        call = await sync_to_async(Call.objects.get)(id=call_id)
        call.accepted = accepted
        await sync_to_async(call.save)()

        await self.channel_layer.group_send(
            f"user_{call.caller.id}",
            {
                "type": "call.answer",
                "call": {
                    "accepted": accepted,
                    "callee": self.user.id,
                },
            }
        )

    async def chat_message(self, event):
        await self.send(text_data=json.dumps({
            "type": "chat_message",
            **event["message"],
        }))

    async def webrtc_offer(self, event):
        await self.send(text_data=json.dumps({
            "type": "webrtc_offer",
            **event["offer"],
        }))

    async def webrtc_answer(self, event):
        await self.send(text_data=json.dumps({
            "type": "webrtc_answer",
            **event["answer"],
        }))

    async def webrtc_ice_candidate(self, event):
        await self.send(text_data=json.dumps({
            "type": "webrtc_ice_candidate",
            **event["ice"],
        }))

    async def call_incoming(self, event):
        await self.send(text_data=json.dumps({
            "type": "incoming_call",
            **event["call"],
        }))

    async def call_answer(self, event):
        await self.send(text_data=json.dumps({
            "type": "call_answer",
            **event["call"],
        }))
