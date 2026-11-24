// In-memory stand-in for the platform keymockStore.
const mockStore = new Map<string, string>();
jest.mock('expo-secure-store', () => ({
  setItemAsync: async (key: string, value: string) => void mockStore.set(key, value),
  getItemAsync: async (key: string) => mockStore.get(key) ?? null,
  deleteItemAsync: async (key: string) => void mockStore.delete(key),
}));

jest.mock('expo-router', () => {
  const router = { push: jest.fn(), replace: jest.fn(), back: jest.fn() };
  return {
    useRouter: () => router,
    useLocalSearchParams: jest.fn(() => ({ id: 'doc-1' })),
    useFocusEffect: jest.fn(),
    __router: router,
  };
});

// Services and screens log failures on purpose; keep test output readable.
jest.spyOn(console, 'warn').mockImplementation(() => undefined);
