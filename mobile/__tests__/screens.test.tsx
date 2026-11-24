import { act, fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import * as ExpoRouter from 'expo-router';
import React from 'react';
import { Alert } from 'react-native';

import { useAuth } from '../src/auth/AuthContext';
import DocumentDetailScreen from '../src/screens/DocumentDetailScreen';
import DocumentListScreen from '../src/screens/DocumentListScreen';
import LoginScreen from '../src/screens/LoginScreen';
import SearchScreen from '../src/screens/SearchScreen';
import TagsScreen from '../src/screens/TagsScreen';
import UploadScreen from '../src/screens/UploadScreen';
import { ApiError } from '../src/services/apiError';
import * as documents from '../src/services/documentService';
import * as statistics from '../src/services/statisticsService';
import * as tags from '../src/services/tagService';
import * as validation from '../src/services/validationService';
import { makeDocument } from './helpers';

jest.mock('../src/services/documentService');
jest.mock('../src/services/tagService');
jest.mock('../src/services/statisticsService');
jest.mock('../src/services/validationService');
jest.mock('../src/auth/AuthContext', () => ({ useAuth: jest.fn() }));
jest.mock('expo-document-picker', () => ({ getDocumentAsync: jest.fn() }));
jest.mock('expo-image-picker', () => ({
  requestCameraPermissionsAsync: jest.fn(),
  launchCameraAsync: jest.fn(),
}));

const doc = jest.mocked(documents);
const tag = jest.mocked(tags);
const stats = jest.mocked(statistics);
const rules = jest.mocked(validation);
const router = (ExpoRouter as unknown as { __router: { push: jest.Mock; replace: jest.Mock; back: jest.Mock } }).__router;

const RULES = { maxTitleLength: 20, maxFileSize: 1000, allowedContentTypes: ['application/pdf'] };
const FINANCE = { id: 't1', name: 'Finance' };

beforeEach(() => {
  jest.resetAllMocks();
  jest.mocked(ExpoRouter.useLocalSearchParams).mockReturnValue({ id: 'doc-1' });
  tag.listTags.mockResolvedValue([FINANCE]);
  rules.getValidationRules.mockResolvedValue(RULES);
  stats.getTagStatistics.mockResolvedValue([]);
});

describe('LoginScreen', () => {
  const auth = (over: Partial<ReturnType<typeof useAuth>>) =>
    jest.mocked(useAuth).mockReturnValue({ status: 'signedOut', error: null, signIn: jest.fn(), signOut: jest.fn(), ...over });

  it('shows a loading state while the session is checked', async () => {
    auth({ status: 'loading' });
    await render(<LoginScreen />);
    expect(screen.getByText('Checking session…')).toBeTruthy();
  });

  it('shows a visible error when login failed', async () => {
    auth({ error: 'Login failed: nope' });
    await render(<LoginScreen />);
    expect(screen.getByText('Login failed: nope')).toBeTruthy();
  });

  it('starts the login flow', async () => {
    const signIn = jest.fn();
    auth({ signIn });
    await render(<LoginScreen />);
    await fireEvent.press(screen.getByText('Log in'));
    expect(signIn).toHaveBeenCalled();
  });
});

describe('DocumentListScreen', () => {
  it('shows loading, then documents', async () => {
    doc.listDocuments.mockResolvedValue([makeDocument({ title: 'Invoice', tagName: 'Finance' })]);
    await render(<DocumentListScreen />);
    expect(await screen.findByText('Invoice')).toBeTruthy();
    await fireEvent.press(screen.getByText('Invoice'));
    expect(router.push).toHaveBeenCalledWith('/document/doc-1');
  });

  it('shows the loading indicator before data arrives', async () => {
    doc.listDocuments.mockReturnValue(new Promise(() => undefined));
    await render(<DocumentListScreen />);
    expect(screen.getByText('Loading documents…')).toBeTruthy();
  });

  it('shows an explicit empty state', async () => {
    doc.listDocuments.mockResolvedValue([]);
    await render(<DocumentListScreen />);
    expect(await screen.findByText('No documents')).toBeTruthy();
  });

  it('shows the error and retries', async () => {
    doc.listDocuments.mockRejectedValueOnce(new ApiError(0, 'Cannot reach the server.'));
    doc.listDocuments.mockResolvedValueOnce([makeDocument()]);
    await render(<DocumentListScreen />);
    expect(await screen.findByText('Cannot reach the server.')).toBeTruthy();
    await fireEvent.press(screen.getByText('Try again'));
    expect(await screen.findByText('Invoice')).toBeTruthy();
  });

  it('filters by tag and by untagged, one at a time', async () => {
    doc.listDocuments.mockResolvedValue([]);
    await render(<DocumentListScreen />);
    await screen.findByText('No documents');
    await fireEvent.press(await screen.findByText('Finance'));
    await waitFor(() => expect(doc.listDocuments).toHaveBeenLastCalledWith({ tag: 't1' }));
    await fireEvent.press(screen.getByText('Untagged'));
    await waitFor(() => expect(doc.listDocuments).toHaveBeenLastCalledWith({ untagged: true }));
    await fireEvent.press(screen.getByText('All'));
    await waitFor(() => expect(doc.listDocuments).toHaveBeenLastCalledWith({}));
  });

  it('opens the upload screen', async () => {
    doc.listDocuments.mockResolvedValue([]);
    await render(<DocumentListScreen />);
    await fireEvent.press(await screen.findByText('Upload document'));
    expect(router.push).toHaveBeenCalledWith('/upload');
  });
});

describe('SearchScreen', () => {
  it('prompts before anything is typed', async () => {
    await render(<SearchScreen />);
    expect(screen.getByText('Search your documents')).toBeTruthy();
    expect(doc.searchDocuments).not.toHaveBeenCalled();
  });

  it('debounces input and shows hits', async () => {
    doc.searchDocuments.mockResolvedValue([makeDocument({ title: 'Rent contract' })]);
    await render(<SearchScreen />);
    await fireEvent.changeText(screen.getByLabelText('Search documents'), 'r');
    await fireEvent.changeText(screen.getByLabelText('Search documents'), 'rent');
    expect(doc.searchDocuments).not.toHaveBeenCalled();
    expect(await screen.findByText('Rent contract')).toBeTruthy();
    expect(doc.searchDocuments).toHaveBeenCalledTimes(1);
    expect(doc.searchDocuments).toHaveBeenCalledWith('rent');
  });

  it('shows an explicit no-results state', async () => {
    doc.searchDocuments.mockResolvedValue([]);
    await render(<SearchScreen />);
    await fireEvent.changeText(screen.getByLabelText('Search documents'), 'zzz');
    expect(await screen.findByText('No results')).toBeTruthy();
  });

  it('shows search errors', async () => {
    doc.searchDocuments.mockRejectedValue(new ApiError(500, 'Search is unavailable.'));
    await render(<SearchScreen />);
    await fireEvent.changeText(screen.getByLabelText('Search documents'), 'x');
    expect(await screen.findByText('Search is unavailable.')).toBeTruthy();
  });
});

describe('DocumentDetailScreen', () => {
  it('shows a loading state', async () => {
    doc.getDocument.mockReturnValue(new Promise(() => undefined));
    await render(<DocumentDetailScreen />);
    expect(screen.getByText('Loading document…')).toBeTruthy();
  });

  it('shows an error', async () => {
    doc.getDocument.mockRejectedValue(new ApiError(404, 'The requested item was not found.'));
    await render(<DocumentDetailScreen />);
    expect(await screen.findByText('The requested item was not found.')).toBeTruthy();
  });

  it('explains a missing summary and refreshes it', async () => {
    doc.getDocument.mockResolvedValueOnce(makeDocument({ summary: null }));
    doc.getDocument.mockResolvedValueOnce(makeDocument({ summary: 'A rent invoice.' }));
    await render(<DocumentDetailScreen />);
    expect(await screen.findByText(/generated in the background/)).toBeTruthy();
    await fireEvent.press(screen.getByText('Refresh'));
    expect(await screen.findByText('A rent invoice.')).toBeTruthy();
  });

  it('shows the metadata', async () => {
    doc.getDocument.mockResolvedValue(makeDocument({ summary: 'S', tagName: 'Finance', tagId: 't1' }));
    await render(<DocumentDetailScreen />);
    expect(await screen.findByText('invoice.pdf')).toBeTruthy();
    expect(screen.getByText('application/pdf')).toBeTruthy();
    expect(screen.getByText('2.0 KB')).toBeTruthy();
  });

  it('saves a new title, with a counter from the server limit', async () => {
    doc.getDocument.mockResolvedValue(makeDocument());
    doc.updateDocumentTitle.mockResolvedValue(makeDocument({ title: 'Renamed' }));
    await render(<DocumentDetailScreen />);
    const input = await screen.findByLabelText('Title');
    await fireEvent.changeText(input, 'Renamed');
    expect(await screen.findByText('7/20')).toBeTruthy();
    await fireEvent.press(screen.getByText('Save title'));
    await waitFor(() => expect(doc.updateDocumentTitle).toHaveBeenCalledWith('doc-1', 'Renamed'));
  });

  it('blocks a title longer than the server limit', async () => {
    doc.getDocument.mockResolvedValue(makeDocument());
    await render(<DocumentDetailScreen />);
    await fireEvent.changeText(await screen.findByLabelText('Title'), 'x'.repeat(25));
    expect(await screen.findByText(/at most 20/)).toBeTruthy();
    await fireEvent.press(screen.getByText('Save title'));
    expect(doc.updateDocumentTitle).not.toHaveBeenCalled();
  });

  it('assigns and removes a tag', async () => {
    doc.getDocument.mockResolvedValue(makeDocument());
    doc.assignTag.mockResolvedValue(makeDocument({ tagId: 't1', tagName: 'Finance' }));
    await render(<DocumentDetailScreen />);
    await fireEvent.press(await screen.findByText('Finance'));
    await waitFor(() => expect(doc.assignTag).toHaveBeenCalledWith('doc-1', 't1'));
    await fireEvent.press(screen.getByText('No tag'));
    await waitFor(() => expect(doc.assignTag).toHaveBeenLastCalledWith('doc-1', null));
  });

  it('deletes only after confirmation', async () => {
    doc.getDocument.mockResolvedValue(makeDocument());
    doc.deleteDocument.mockResolvedValue(undefined);
    const alert = jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    await render(<DocumentDetailScreen />);
    await fireEvent.press(await screen.findByText('Delete document'));
    expect(doc.deleteDocument).not.toHaveBeenCalled();
    const buttons = alert.mock.calls[0][2] ?? [];
    await act(async () => buttons.find((b) => b.style === 'destructive')?.onPress?.());
    expect(doc.deleteDocument).toHaveBeenCalledWith('doc-1');
    expect(router.back).toHaveBeenCalled();
  });

  it('shows server errors from actions', async () => {
    doc.getDocument.mockResolvedValue(makeDocument());
    doc.assignTag.mockRejectedValue(new ApiError(404, 'Tag not found'));
    await render(<DocumentDetailScreen />);
    await fireEvent.press(await screen.findByText('Finance'));
    expect(await screen.findByText('Tag not found')).toBeTruthy();
  });
});

describe('UploadScreen', () => {
  const pdf = { uri: 'file:///a.pdf', name: 'scan.pdf', mimeType: 'application/pdf', size: 500 };

  it('shows loading and error states for the rules', async () => {
    rules.getValidationRules.mockReturnValue(new Promise(() => undefined));
    await render(<UploadScreen />);
    expect(screen.getByText('Loading upload rules…')).toBeTruthy();
  });

  it('shows an error when the rules cannot be fetched', async () => {
    rules.getValidationRules.mockRejectedValue(new ApiError(0, 'Cannot reach the server.'));
    await render(<UploadScreen />);
    expect(await screen.findByText('Cannot reach the server.')).toBeTruthy();
  });

  it('requires a file before uploading', async () => {
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Upload'));
    expect(await screen.findByText(/take a photo or pick a file/)).toBeTruthy();
    expect(doc.uploadDocument).not.toHaveBeenCalled();
  });

  it('uploads a picked file with a suggested title and chosen tag', async () => {
    jest.mocked(DocumentPicker.getDocumentAsync).mockResolvedValue({ canceled: false, assets: [pdf] } as never);
    doc.uploadDocument.mockResolvedValue(makeDocument({ id: 'new-1' }));
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Choose file'));
    expect(await screen.findByDisplayValue('scan')).toBeTruthy();
    await fireEvent.press(await screen.findByText('Finance'));
    await fireEvent.press(screen.getByText('Upload'));
    await waitFor(() =>
      expect(doc.uploadDocument).toHaveBeenCalledWith(
        { title: 'scan', tagId: 't1' },
        { uri: 'file:///a.pdf', name: 'scan.pdf', type: 'application/pdf' },
      ),
    );
    expect(router.replace).toHaveBeenCalledWith('/document/new-1');
  });

  it('validates against the server rules before sending', async () => {
    jest
      .mocked(DocumentPicker.getDocumentAsync)
      .mockResolvedValue({ canceled: false, assets: [{ ...pdf, size: 5000 }] } as never);
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Choose file'));
    await fireEvent.press(await screen.findByText('Upload'));
    expect(await screen.findByText(/maximum/)).toBeTruthy();
    expect(doc.uploadDocument).not.toHaveBeenCalled();
  });

  it('shows the server error when the upload fails', async () => {
    jest.mocked(DocumentPicker.getDocumentAsync).mockResolvedValue({ canceled: false, assets: [pdf] } as never);
    doc.uploadDocument.mockRejectedValue(new ApiError(415, 'That file type is not supported.'));
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Choose file'));
    await fireEvent.press(await screen.findByText('Upload'));
    expect(await screen.findByText('That file type is not supported.')).toBeTruthy();
  });

  it('ignores a cancelled picker', async () => {
    jest.mocked(DocumentPicker.getDocumentAsync).mockResolvedValue({ canceled: true, assets: null } as never);
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Choose file'));
    expect(screen.queryByText(/Selected:/)).toBeNull();
  });

  it('explains a denied camera permission', async () => {
    jest.mocked(ImagePicker.requestCameraPermissionsAsync).mockResolvedValue({ granted: false } as never);
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Take photo'));
    expect(await screen.findByText(/Camera permission was denied/)).toBeTruthy();
    expect(ImagePicker.launchCameraAsync).not.toHaveBeenCalled();
  });

  it('takes a photo and rejects it when images are not allowed', async () => {
    jest.mocked(ImagePicker.requestCameraPermissionsAsync).mockResolvedValue({ granted: true } as never);
    jest
      .mocked(ImagePicker.launchCameraAsync)
      .mockResolvedValue({ canceled: false, assets: [{ uri: 'file:///p.jpg', fileName: 'p.jpg', mimeType: 'image/jpeg' }] } as never);
    await render(<UploadScreen />);
    await fireEvent.press(await screen.findByText('Take photo'));
    expect(await screen.findByText(/Selected: p.jpg/)).toBeTruthy();
    await fireEvent.press(screen.getByText('Upload'));
    expect(await screen.findByText(/image\/jpeg.*not allowed/)).toBeTruthy();
  });
});

describe('TagsScreen', () => {
  it('shows a loading state', async () => {
    tag.listTags.mockReturnValue(new Promise(() => undefined));
    await render(<TagsScreen />);
    expect(screen.getByText('Loading tags…')).toBeTruthy();
  });

  it('shows an error', async () => {
    tag.listTags.mockRejectedValue(new ApiError(0, 'Cannot reach the server.'));
    await render(<TagsScreen />);
    expect(await screen.findByText('Cannot reach the server.')).toBeTruthy();
  });

  it('shows an explicit empty state', async () => {
    tag.listTags.mockResolvedValue([]);
    await render(<TagsScreen />);
    expect(await screen.findByText('No tags yet')).toBeTruthy();
  });

  it('lists tags with document counts and untagged total', async () => {
    stats.getTagStatistics.mockResolvedValue([
      { tagId: 't1', tagName: 'Finance', documentCount: 3 },
      { tagId: null, tagName: 'Untagged', documentCount: 2 },
    ]);
    await render(<TagsScreen />);
    expect(await screen.findByText('3 documents')).toBeTruthy();
    expect(screen.getByText('Untagged documents: 2')).toBeTruthy();
  });

  it('defaults to zero documents for a tag missing from the statistics', async () => {
    await render(<TagsScreen />);
    expect(await screen.findByText('0 documents')).toBeTruthy();
  });

  it('creates a tag', async () => {
    tag.createTag.mockResolvedValue({ id: 't2', name: 'Legal' });
    await render(<TagsScreen />);
    await fireEvent.changeText(await screen.findByLabelText('New tag name'), ' Legal ');
    await fireEvent.press(screen.getByText('Add'));
    await waitFor(() => expect(tag.createTag).toHaveBeenCalledWith('Legal'));
  });

  it("shows the server's message for a duplicate tag", async () => {
    tag.createTag.mockRejectedValue(new ApiError(409, 'Tag already exists: Finance'));
    await render(<TagsScreen />);
    await fireEvent.changeText(await screen.findByLabelText('New tag name'), 'finance');
    await fireEvent.press(screen.getByText('Add'));
    expect(await screen.findByText('Tag already exists: Finance')).toBeTruthy();
  });

  it("shows the server's message when deleting a tag that is in use", async () => {
    tag.deleteTag.mockRejectedValue(new ApiError(409, 'Tag is still assigned to documents'));
    const alert = jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    await render(<TagsScreen />);
    await fireEvent.press(await screen.findByText('Delete'));
    const buttons = alert.mock.calls[0][2] ?? [];
    await act(async () => buttons.find((b) => b.style === 'destructive')?.onPress?.());
    expect(await screen.findByText('Tag is still assigned to documents')).toBeTruthy();
    expect(tag.deleteTag).toHaveBeenCalledWith('t1');
  });

  it('deletes an unused tag', async () => {
    tag.deleteTag.mockResolvedValue(undefined);
    const alert = jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    await render(<TagsScreen />);
    await fireEvent.press(await screen.findByText('Delete'));
    const buttons = alert.mock.calls[0][2] ?? [];
    await act(async () => buttons.find((b) => b.style === 'destructive')?.onPress?.());
    await waitFor(() => expect(tag.deleteTag).toHaveBeenCalledWith('t1'));
  });
});
