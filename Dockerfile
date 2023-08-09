# Use an official Anaconda runtime as the base image
FROM tensorflow/tensorflow

RUN apt-get update && apt-get install libgl1 -y
    
COPY . /app

WORKDIR /app

# Install any needed packages specified in requirements.txt
RUN pip install -r requirements.txt

# The code to run when container is started:
ENTRYPOINT ["python", "Python/build_sqlite.py", "-i", "Python/datasetImages"]
