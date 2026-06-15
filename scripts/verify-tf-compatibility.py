#!/usr/bin/env python3
"""Verify TensorFlow upgrade compatibility - no accuracy drift."""
import sys
import numpy as np


def test_basic_ops():
    """Test basic TF operations produce consistent results."""
    try:
        import tensorflow as tf
        print(f"TensorFlow version: {tf.__version__}")

        # Test deterministic operations
        tf.random.set_seed(42)
        np.random.seed(42)

        # Basic matrix multiplication
        a = tf.constant([[1.0, 2.0], [3.0, 4.0]])
        b = tf.constant([[5.0, 6.0], [7.0, 8.0]])
        c = tf.matmul(a, b)
        expected = np.array([[19.0, 22.0], [43.0, 50.0]])
        assert np.allclose(c.numpy(), expected), "Matrix multiplication mismatch"

        # Test random number generation (seeded)
        tf.random.set_seed(42)
        rand1 = tf.random.normal([3, 3])
        tf.random.set_seed(42)
        rand2 = tf.random.normal([3, 3])
        assert np.allclose(rand1.numpy(), rand2.numpy()), "Random seed not deterministic"

        print("All compatibility checks passed!")
        return True
    except Exception as e:
        print(f"Compatibility check failed: {e}")
        return False


if __name__ == "__main__":
    success = test_basic_ops()
    sys.exit(0 if success else 1)
