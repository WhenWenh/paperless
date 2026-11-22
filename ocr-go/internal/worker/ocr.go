package worker

import (
	"context"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
)

func extensionFor(contentType string) string {
	switch strings.ToLower(contentType) {
	case "application/pdf":
		return ".pdf"
	case "image/png":
		return ".png"
	case "image/jpeg":
		return ".jpg"
	case "image/tiff":
		return ".tiff"
	case "image/bmp":
		return ".bmp"
	case "image/gif":
		return ".gif"
	case "image/webp":
		return ".webp"
	default:
		return ""
	}
}

func extractText(ctx context.Context, document io.Reader, contentType string) (string, error) {
	extension := extensionFor(contentType)
	if extension == "" {
		return "", fmt.Errorf("unsupported OCR content type %q", contentType)
	}
	directory, err := os.MkdirTemp("", "paperless-ocr-")
	if err != nil {
		return "", fmt.Errorf("create OCR workspace: %w", err)
	}
	defer func() {
		if err := os.RemoveAll(directory); err != nil {
			slog.Warn("Could not clean OCR workspace", "error", err)
		}
	}()

	input := filepath.Join(directory, "input"+extension)
	file, err := os.Create(input)
	if err != nil {
		return "", fmt.Errorf("create OCR input: %w", err)
	}
	_, copyErr := io.Copy(file, document)
	if err := errors.Join(copyErr, file.Close()); err != nil {
		return "", fmt.Errorf("write OCR input: %w", err)
	}

	if extension == ".pdf" {
		return extractPDF(ctx, directory, input)
	}
	return runCommand(ctx, "tesseract", input, "stdout")
}

func extractPDF(ctx context.Context, directory, input string) (string, error) {
	if _, err := runCommand(ctx, "pdftoppm", "-png", input, filepath.Join(directory, "page")); err != nil {
		return "", err
	}

	files, err := os.ReadDir(directory)
	if err != nil {
		return "", fmt.Errorf("list converted PDF pages: %w", err)
	}
	type page struct {
		name   string
		number int
	}
	var pages []page
	for _, file := range files {
		name := file.Name()
		if file.IsDir() || !strings.HasPrefix(name, "page-") || !strings.HasSuffix(name, ".png") {
			continue
		}
		number, err := strconv.Atoi(strings.TrimSuffix(strings.TrimPrefix(name, "page-"), ".png"))
		if err != nil {
			return "", fmt.Errorf("parse PDF page number: %w", err)
		}
		pages = append(pages, page{name, number})
	}
	if len(pages) == 0 {
		return "", errors.New("PDF conversion produced no page images")
	}
	sort.Slice(pages, func(i, j int) bool { return pages[i].number < pages[j].number })

	var text strings.Builder
	for _, page := range pages {
		output, err := runCommand(ctx, "tesseract", filepath.Join(directory, page.name), "stdout")
		if err != nil {
			return "", fmt.Errorf("recognize PDF page %d: %w", page.number, err)
		}
		text.WriteString(output)
		text.WriteByte('\n')
	}
	return text.String(), nil
}

func runCommand(ctx context.Context, name string, args ...string) (string, error) {
	output, err := exec.CommandContext(ctx, name, args...).Output()
	if ctx.Err() != nil {
		return "", ctx.Err()
	}
	if err != nil {
		var exitError *exec.ExitError
		if errors.As(err, &exitError) {
			return "", fmt.Errorf("%s: %w: %s", name, err, strings.TrimSpace(string(exitError.Stderr)))
		}
		return "", fmt.Errorf("%s: %w", name, err)
	}
	return string(output), nil
}
