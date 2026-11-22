import { useFocusEffect } from 'expo-router';
import { useCallback, useRef } from 'react';

/** Reloads when the screen regains focus (not on the first focus, which loads anyway). */
export function useReloadOnFocus(reload: () => Promise<void>): void {
  const first = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (first.current) {
        first.current = false;
        return;
      }
      void reload();
    }, [reload]),
  );
}
