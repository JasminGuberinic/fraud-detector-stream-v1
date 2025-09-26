#!/bin/bash

echo "🚀 Setting up Fraud Model Generator..."

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating Python virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "🔄 Activating virtual environment..."
source venv/bin/activate

# Install Python dependencies
echo "⬇️ Installing Python dependencies..."
pip install numpy scikit-learn

# Run the generator
echo "🤖 Running model generator..."
python generate_fraud_model.py

if [ $? -eq 0 ]; then
    echo "✅ Model generated successfully!"
    echo "📁 Location: models/fraud_model.bin"
    echo ""
    echo "💡 To use virtual environment again:"
    echo "   source venv/bin/activate"
else
    echo "❌ Failed to generate model"
    exit 1
fi

# Deactivate virtual environment
deactivate