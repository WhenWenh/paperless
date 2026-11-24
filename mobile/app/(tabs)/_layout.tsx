import { Tabs } from 'expo-router';
import React from 'react';
import { Pressable, Text, type ColorValue } from 'react-native';

import { useAuth } from '../../src/auth/AuthContext';
import { colors } from '../../src/theme';

/** Simple glyph icons; avoids pulling in an icon font package. */
function TabIcon({ glyph, color }: { glyph: string; color: ColorValue }) {
  return <Text style={{ fontSize: 20, color }}>{glyph}</Text>;
}

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
      <Tabs.Screen
        name="index"
        options={{
          title: 'Documents',
          tabBarIcon: ({ color }) => <TabIcon glyph="📄" color={color} />
        }}
      />
      <Tabs.Screen
        name="search"
        options={{
          title: 'Search',
          tabBarIcon: ({ color }) => <TabIcon glyph="🔍" color={color} />
        }}
      />
      <Tabs.Screen
        name="tags"
        options={{
          title: 'Tags',
          tabBarIcon: ({ color }) => <TabIcon glyph="🏷" color={color} />
        }}
      />
    </Tabs>
  );
}
