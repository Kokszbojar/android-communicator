from django.db import models
from django.conf import settings


class Message(models.Model):
    sender = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="sent_messages")
    recipient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="received_messages")
    content = models.TextField()
    timestamp = models.DateTimeField(auto_now_add=True)
    is_read = models.BooleanField(default=False)

    def __str__(self):
        return f"{self.pk} | {self.sender} -> {self.recipient}"


class Call(models.Model):
    caller = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="outgoing_calls")
    callee = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="incoming_calls")
    created_at = models.DateTimeField(auto_now_add=True)
    accepted = models.BooleanField(null=True)  # None = oczekuje, True = zaakceptowano, False = odrzucono

    def __str__(self):
        return f"{self.pk} | {self.caller} -> {self.callee} ({'accepted' if self.accepted else 'pending'})"
