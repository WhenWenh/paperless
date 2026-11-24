import { useRouter } from 'expo-router';
import React, { useCallback } from 'react';
import { FlatList, StyleSheet, TextInput, View } from 'react-native';

import { DocumentRow } from '../components/DocumentRow';
import { EmptyView, ErrorView, LoadingView } from '../components/StateViews';
import { useAsync } from '../hooks/useAsync';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { documentService } from '../services';
import { colors } from '../theme';

export const SEARCH_DEBOUNCE_MS = 400;

export default function SearchScreen() {
  const router = useRouter();
  const [text, setText] = React.useState('');
  const query = useDebouncedValue(text.trim(), SEARCH_DEBOUNCE_MS);

  const search = useCallback(
    () => (query ? documentService.searchDocuments(query) : Promise.resolve([])),
    [query],
  );
  const results = useAsync(search);

  const waiting = text.trim() !== query; // typed but not yet searched
  let body: React.ReactNode;
  if (!query && !waiting) {
    body = <EmptyView title="Search your documents" hint="Type a word to search titles and content." />;
  } else if (results.loading || waiting) {
    body = <LoadingView label="Searching…" />;
  } else if (results.error) {
    body = <ErrorView message={results.error} onRetry={() => void results.reload()} />;
  } else if (!results.data?.length) {
    body = <EmptyView title="No results" hint={`Nothing found for “${query}”.`} />;
  } else {
    body = (
      <FlatList
        data={results.data}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        ItemSeparatorComponent={() => <View style={styles.separator} />}
        renderItem={({ item }) => <DocumentRow document={item} onPress={() => router.push(`/document/${item.id}`)} />}
      />
    );
  }

  return (
    <View style={styles.container}>
      <TextInput
        accessibilityLabel="Search documents"
        placeholder="Search documents"
        value={text}
        onChangeText={setText}
        autoCapitalize="none"
        autoCorrect={false}
        returnKeyType="search"
        style={styles.input}
      />
      <View style={styles.body}>{body}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  input: { margin: 12, padding: 12, backgroundColor: colors.card, borderRadius: 8, borderWidth: 1, borderColor: colors.border, fontSize: 16 },
  body: { flex: 1 },
  list: { padding: 12 },
  separator: { height: 8 },
});
