global:
  smtp_smarthost: '${SMTP_SMARTHOST}'
  smtp_from: '${SMTP_FROM}'
  smtp_auth_username: '${SMTP_AUTH_USERNAME}'
  smtp_auth_password: '${SMTP_AUTH_PASSWORD}'
  smtp_require_tls: true

route:
  receiver: operations-notifications
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 2h

receivers:
  - name: operations-notifications
    email_configs:
      - to: '${ALERT_EMAIL_TO}'
        send_resolved: true
    webhook_configs:
      - url: '${ALERT_WEBHOOK_URL}'
        send_resolved: true
