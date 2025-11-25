import { StatusBar } from 'expo-status-bar'
import { useState } from 'react'
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Platform,
  Alert,
} from 'react-native'
import { showOverlay } from '@amehmeto/tied-siren-blocking-overlay'

export default function App() {
  const [status, setStatus] = useState('Ready')
  const [error, setError] = useState(null)

  const testOverlay = async () => {
    if (Platform.OS !== 'android') {
      Alert.alert('Android Only', 'This feature only works on Android devices')
      return
    }

    setStatus('Launching overlay...')
    setError(null)

    try {
      await showOverlay('com.example.blocked')
      setStatus('Overlay launched successfully!')
    } catch (err) {
      setError(err.message || 'Unknown error')
      setStatus('Failed to launch overlay')
      console.error('Overlay error:', err)
    }
  }

  const testInvalidPackage = async () => {
    if (Platform.OS !== 'android') {
      Alert.alert('Android Only', 'This feature only works on Android devices')
      return
    }

    setStatus('Testing invalid package...')
    setError(null)

    try {
      await showOverlay('')
      setStatus('Should have thrown error!')
    } catch (err) {
      if (err.code === 'ERR_INVALID_PACKAGE') {
        setStatus('Correctly caught ERR_INVALID_PACKAGE')
        setError(err.message)
      } else {
        setStatus('Unexpected error')
        setError(err.message)
      }
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Blocking Overlay Test</Text>

      <View style={styles.statusContainer}>
        <Text style={styles.statusLabel}>Status:</Text>
        <Text style={styles.statusText}>{status}</Text>
        {error && <Text style={styles.errorText}>Error: {error}</Text>}
      </View>

      <TouchableOpacity style={styles.button} onPress={testOverlay}>
        <Text style={styles.buttonText}>Test Overlay</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, styles.secondaryButton]}
        onPress={testInvalidPackage}
      >
        <Text style={styles.buttonText}>Test Invalid Package</Text>
      </TouchableOpacity>

      <Text style={styles.hint}>
        {Platform.OS === 'android'
          ? 'Tap "Test Overlay" to launch the blocking screen'
          : 'Run on Android to test the overlay'}
      </Text>

      <StatusBar style="light" />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a2e',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 40,
  },
  statusContainer: {
    backgroundColor: '#16213e',
    padding: 20,
    borderRadius: 10,
    width: '100%',
    marginBottom: 30,
  },
  statusLabel: {
    color: '#888',
    fontSize: 14,
    marginBottom: 5,
  },
  statusText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '500',
  },
  errorText: {
    color: '#ff6b6b',
    fontSize: 14,
    marginTop: 10,
  },
  button: {
    backgroundColor: '#e94560',
    paddingVertical: 15,
    paddingHorizontal: 40,
    borderRadius: 10,
    marginBottom: 15,
    width: '100%',
    alignItems: 'center',
  },
  secondaryButton: {
    backgroundColor: '#0f3460',
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  hint: {
    color: '#888',
    fontSize: 14,
    marginTop: 30,
    textAlign: 'center',
  },
})
