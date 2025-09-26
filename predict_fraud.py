#!/usr/bin/env python3
import sys
import pickle
import numpy as np

def predict_fraud(features):
    try:
        # Load the sklearn model
        with open('models/fraud_model.bin', 'rb') as f:
            model = pickle.load(f)

        # Convert features to numpy array
        features_array = np.array([features])

        # Get probability of fraud (class 1)
        fraud_probability = model.predict_proba(features_array)[0][1]

        return fraud_probability

    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 0.1  # fallback value

if __name__ == "__main__":
    if len(sys.argv) != 7:
        print("Usage: python predict_fraud.py <amount> <high_risk_country> <hour> <day_of_week> <is_mobile> <transaction_type>")
        sys.exit(1)

    try:
        features = [float(arg) for arg in sys.argv[1:]]
        result = predict_fraud(features)
        print(f"{result:.6f}")
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)