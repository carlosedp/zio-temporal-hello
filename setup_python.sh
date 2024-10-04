#!/bin/bash

# Setup Python environment for sample client

# Check if Python 3 is installed
if ! command -v python3 &>/dev/null; then
  echo "Python 3 is not installed. Please install Python 3 and try again."
  exit
fi

# Check if pip and venv are installed
if ! command -v python3 -m pip &>/dev/null; then
  echo "pip is not installed. Please install pip and try again."
  exit
fi

if ! command -v python3 -m venv &>/dev/null; then
  echo "venv is not installed. Please install venv and try again."
  exit
fi

# Create a virtual environment
python3 -m venv venv

# Activate the virtual environment
source venv/bin/activate

# Install required packages
python -m pip install temporalio

# Deactivate the virtual environment
deactivate

echo ""
echo "Python environment setup complete. Run 'source venv/bin/activate' to activate the virtual environment."
echo "To run the sample client, make sure Temporal and the Worker are running and execute 'python3 sample_client.py'."
