import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import type { TagResponse } from '../models';
import { colors } from '../theme';

interface Props {
  tags: TagResponse[];
  selectedId: string | null;
  onSelect: (tagId: string | null) => void;
  noneLabel?: string;
}

/** Horizontal single-select chips with a leading "no tag" option. */
export function TagPicker({ tags, selectedId, onSelect, noneLabel = 'No tag' }: Props) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
      <Chip label={noneLabel} selected={selectedId === null} onPress={() => onSelect(null)} />
      {tags.map((tag) => (
        <Chip key={tag.id} label={tag.name} selected={selectedId === tag.id} onPress={() => onSelect(tag.id)} />
      ))}
    </ScrollView>
  );
}

export function Chip({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[styles.chip, selected && styles.chipSelected]}
    >
      <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { gap: 8, paddingVertical: 4 },
  chip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 999, backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border },
  chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { color: colors.text },
  chipTextSelected: { color: colors.primaryText, fontWeight: '600' },
});
