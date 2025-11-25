#!/bin/bash
set -e

gcloud functions deploy notify-admin     --runtime java17     --trigger-topic feedback-alerts     --entry-point fiap_adj8.feedback_platform.infra.adapter.in.NotifyAdminFunction     --region us-central1
