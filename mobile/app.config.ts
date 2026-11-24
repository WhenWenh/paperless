import type { ExpoConfig } from 'expo/config';

// Single place to point the app at a different backend, e.g. a LAN IP for a
// physical device: EXPO_PUBLIC_API_URL=http://192.168.1.20:8081 npx expo start
const apiBaseUrl = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8081';
const keycloakBaseUrl = process.env.EXPO_PUBLIC_KEYCLOAK_URL ?? 'http://localhost:9445';

const config: ExpoConfig = {
  name: 'Paperless',
  slug: 'paperless-mobile',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  scheme: 'paperlessmobile',
  userInterfaceStyle: 'light',
  ios: {
    supportsTablet: false,
    bundleIdentifier: 'com.paperless.mobile',
    infoPlist: {
      // Local development backend is plain http
      NSAppTransportSecurity: { NSAllowsArbitraryLoads: true },
    },
  },
  android: {
    package: 'com.paperless.mobile',
    adaptiveIcon: {
      backgroundColor: '#E6F4FE',
      foregroundImage: './assets/android-icon-foreground.png',
      backgroundImage: './assets/android-icon-background.png',
      monochromeImage: './assets/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  plugins: [
    'expo-router',
    'expo-secure-store',
    'expo-web-browser',
    // Local development backend is plain http
    ['expo-build-properties', { android: { usesCleartextTraffic: true } }],
    [
      'expo-image-picker',
      { cameraPermission: 'Paperless needs the camera to photograph documents for upload.' },
    ],
  ],
  extra: { apiBaseUrl, keycloakBaseUrl },
};

export default config;
