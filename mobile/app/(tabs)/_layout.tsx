import { Tabs } from 'expo-router';
import React from 'react';
import { Pressable, Text } from 'react-native';

import { useAuth } from '../../src/auth/AuthContext';
import { colors } from '../../src/theme';

function LogoutButton() {
  const { signOut } = useAuth();
  return (
    <Pressable accessibilityRole="button" onPress={() => void signOut()} style={{ paddingHorizontal: 16 }}>
      <Text style={{ color: colors.primary, fontWeight: '600' }}>Log out</Text>
    </Pressable>
  );
}

export default function TabsLayout() {
  return (
    <Tabs screenOptions={{ headerRight: () => <LogoutButton />, tabBarActiveTintColor: colors.primary }}>
      <Tabs.Screen name="index" options={{ title: 'Documents' }} />
      <Tabs.Screen name="search" options={{ title: 'Search' }} />
      <Tabs.Screen name="tags" options={{ title: 'Tags' }} />
    </Tabs>
  );
}
