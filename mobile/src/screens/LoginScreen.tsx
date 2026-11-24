import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { LoadingView } from '../components/StateViews';
import { colors } from '../theme';

export default function LoginScreen() {
  const { status, error, signIn } = useAuth();
  if (status === 'loading') return <LoadingView label="Checking session…" />;
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Paperless</Text>
      <Text style={styles.subtitle}>Sign in to access your documents.</Text>
      {error ? (
        <Text accessibilityRole="alert" style={styles.error}>
          {error}
        </Text>
      ) : null}
      <Button title="Log in" onPress={() => void signIn()} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, gap: 16, backgroundColor: colors.background },
  title: { fontSize: 32, fontWeight: '700', color: colors.text, textAlign: 'center' },
  subtitle: { color: colors.muted, textAlign: 'center' },
  error: { color: colors.danger, textAlign: 'center' },
});
