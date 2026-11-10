FROM python:3.12-slim

RUN apt-get update \
    && apt-get install --no-install-recommends -y ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY dependencies.txt .
RUN pip install --no-cache-dir -r dependencies.txt
COPY . .

ENTRYPOINT ["python", "main.py"]
