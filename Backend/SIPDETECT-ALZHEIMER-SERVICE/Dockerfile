FROM python:3.10-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    cmake \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --upgrade pip

# PyTorch CPU (image plus légère, build plus rapide)
RUN pip install --no-cache-dir \
    torch==2.6.0 torchvision==0.21.0 \
    --index-url https://download.pytorch.org/whl/cpu

COPY Backend/SIPDETECT-ALZHEIMER-SERVICE/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY Backend/SIPDETECT-ALZHEIMER-SERVICE/app ./app

RUN mkdir -p /app/models
COPY sipdetect_ms2_package/model.pth /app/models/model.pth
COPY sipdetect_ms2_package/model_card.json /app/models/model_card.json
COPY sipdetect_ms2_package/results.json /app/models/results.json

ENV PYTHONUNBUFFERED=1
ENV PORT=8087
ENV SIPDETECT_MODEL_PATH=/app/models/model.pth
ENV SIPDETECT_MODEL_DIR=/app/models

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=120s \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8087/health')" || exit 1

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8087"]
