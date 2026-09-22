package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.service.TagService;
import at.fhtw.swen.paperless.api.service.dto.TagDto;
import at.fhtw.swen.paperless.api.service.exception.TagAlreadyExistsException;
import at.fhtw.swen.paperless.api.service.exception.TagInUseException;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import at.fhtw.swen.paperless.api.service.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TagServiceImplTest {

    private final TagRepository tagRepository = mock(TagRepository.class);

    private final TagService tagService =
            new TagServiceImpl(tagRepository, new TagMapper());

    @Test
    void createTag_shouldTrimNameAndSaveTag() {
        UUID id = UUID.randomUUID();
        when(tagRepository.existsByNameIgnoreCase("Finance")).thenReturn(false);
        when(tagRepository.save(any(Tag.class)))
                .thenReturn(Tag.builder().id(id).name("Finance").build());

        TagDto result = tagService.createTag("  Finance  ");

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Finance");
        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Finance");
    }

    @Test
    void createTag_shouldRejectDuplicateName() {
        when(tagRepository.existsByNameIgnoreCase("finance")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag("finance"))
                .isInstanceOf(TagAlreadyExistsException.class)
                .hasMessageContaining("finance");
        verify(tagRepository, never()).save(any());
    }

    @Test
    void getTagById_shouldReturnTag() {
        UUID id = UUID.randomUUID();
        when(tagRepository.findById(id))
                .thenReturn(Optional.of(Tag.builder().id(id).name("Finance").build()));

        Optional<TagDto> result = tagService.getTagById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Finance");
    }

    @Test
    void getTagById_shouldReturnEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(tagRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(tagService.getTagById(id)).isEmpty();
    }

    @Test
    void getTagByName_shouldReturnTag() {
        when(tagRepository.findByName("Finance"))
                .thenReturn(Optional.of(Tag.builder().name("Finance").build()));

        Optional<TagDto> result = tagService.getTagByName("Finance");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Finance");
    }

    @Test
    void getAllTags_shouldReturnTagsSortedAlphabetically() {
        when(tagRepository.findAll()).thenReturn(List.of(
                Tag.builder().name("Work").build(),
                Tag.builder().name("finance").build()));

        List<TagDto> result = tagService.getAllTags();

        assertThat(result).extracting(TagDto::getName)
                .containsExactly("finance", "Work");
    }

    @Test
    void deleteTag_shouldDeleteUnusedTag() {
        UUID id = UUID.randomUUID();
        when(tagRepository.existsById(id)).thenReturn(true);
        when(tagRepository.countDocumentsByTagId(id)).thenReturn(0L);

        tagService.deleteTag(id);

        verify(tagRepository).deleteById(id);
    }

    @Test
    void deleteTag_shouldThrowWhenTagDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(tagRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> tagService.deleteTag(id))
                .isInstanceOf(TagNotFoundException.class);
        verify(tagRepository, never()).deleteById(any());
    }

    @Test
    void deleteTag_shouldThrowWhenTagIsStillAssigned() {
        UUID id = UUID.randomUUID();
        when(tagRepository.existsById(id)).thenReturn(true);
        when(tagRepository.countDocumentsByTagId(id)).thenReturn(3L);

        assertThatThrownBy(() -> tagService.deleteTag(id))
                .isInstanceOf(TagInUseException.class)
                .hasMessageContaining("3 document");
        verify(tagRepository, never()).deleteById(any());
    }
}
