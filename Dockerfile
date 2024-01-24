# Use an official Anaconda runtime as the base image
#FROM tensorflow/tensorflow
FROM python:3.11-slim

# Install system dependencies
RUN apt-get update && apt-get install -y libgl1 build-essential gcc g++ cmake libglib2.0-0

# Install TensorFlow
RUN python -m pip install tensorflow

# Create and set the working directory
WORKDIR /app

# Copy only the requirements file first to leverage Docker cache
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the entire application
COPY . .

# The code to run when the container is started:
ENTRYPOINT ["python", "Python/build_sqlite.py", "-g"]
# default argument
CMD ["Florence"]

