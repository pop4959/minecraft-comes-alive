import os

import openai
from fastapi import FastAPI

app = FastAPI()

openai.api_key = os.getenv("OPENAI_API_KEY")

model = "text-curie-001"


@app.get("/chat")
def chat(prompt: str, player: str, villager: str):
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
