# Use an official Anaconda runtime as the base image
#FROM tensorflow/tensorflow
FROM python:3.11-slim

RUN apt-get update && apt-get install -y libgl1  build-essential gcc g++ cmake libglib2.0-0

RUN python -m pip install tensorflow

COPY . /app

WORKDIR /app

# Install any needed packages specified in requirements.txt
RUN pip install -r requirements.txt

# The code to run when container is started:
ENTRYPOINT ["python", "Python/build_sqlite.py", "-i", "Python/datasetImages"]
