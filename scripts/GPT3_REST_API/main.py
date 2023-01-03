import os

import openai
from fastapi import FastAPI
from pyrate_limiter import Duration, Limiter, RequestRate, BucketFullException

app = FastAPI()

openai.api_key = os.getenv("OPENAI_API_KEY")

model = "text-curie-001"

limiter = Limiter(RequestRate(800, Duration.HOUR))


@app.get("/chat")
def chat(prompt: str, player: str, villager: str):
    try:
        for i in range(len(prompt) // 100 + 1):
            limiter.try_acquire(player)
        print(limiter.get_current_volume(player))

        response = openai.Completion.create(
            model=model,
            prompt=prompt,
            temperature=0.9,
            max_tokens=150,
            top_p=1,
            frequency_penalty=0.0,
            presence_penalty=0.6,
            stop=[f"{player}:", f"{villager}:"]
        )

        return {"answer": response["choices"][0]["text"]}
    except BucketFullException:
        return {"answer": "(You exceeded your hourly rate, give the AI some rest! Also make sure to use the newest version for best results!)"}
