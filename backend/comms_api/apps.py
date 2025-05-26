from django.apps import AppConfig


class CommsApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'comms_api'

    def ready(self):
        from . import firebase
