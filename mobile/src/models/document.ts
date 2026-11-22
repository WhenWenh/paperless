export interface DocumentResponse {
  id: string;
  title: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  summary: string | null;
  createdAt: string;
  updatedAt: string;
  tagId: string | null;
  tagName: string | null;
}

export interface UpdateDocumentRequest {
  title: string;
}

export interface AssignTagRequest {
  tagId: string | null;
}

export interface DocumentUploadMetadata {
  title: string;
  tagId: string | null;
}

/** React Native file descriptor as produced by the image/document pickers. */
export interface UploadFile {
  uri: string;
  name: string;
  type: string;
}

export interface DocumentFilter {
  tag?: string;
  untagged?: boolean;
}
