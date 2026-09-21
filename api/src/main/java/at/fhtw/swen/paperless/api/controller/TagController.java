package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.controller.request.CreateTagRequest;
import at.fhtw.swen.paperless.api.controller.response.TagResponse;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    public ResponseEntity<TagResponse> createTag(@RequestBody CreateTagRequest request) {
        Tag tag = tagService.createTag(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(toTagResponse(tag));
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags().stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }

    private TagResponse toTagResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
