package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.service.TagService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TagServiceImplTest {

    private final TagRepository tagRepository = mock(TagRepository.class);

    private final TagService tagService = new TagServiceImpl(tagRepository);

    @Test
    void createTag_shouldCreateAndSaveTag() {
        Tag savedTag = new Tag();
        savedTag.setName("Finance");
        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        Tag result = tagService.createTag("Finance");

        assertThat(result.getName()).isEqualTo("Finance");
        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Finance");
    }

    @Test
    void getTagById_shouldReturnTag() {
        UUID id = UUID.randomUUID();
        Tag tag = new Tag();
        tag.setName("Finance");
        when(tagRepository.findById(id)).thenReturn(Optional.of(tag));

        Optional<Tag> result = tagService.getTagById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Finance");
        verify(tagRepository).findById(id);
    }

    @Test
    void getTagById_shouldReturnEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(tagRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(tagService.getTagById(id)).isEmpty();
        verify(tagRepository).findById(id);
    }

    @Test
    void getTagByName_shouldReturnTag() {
        Tag tag = new Tag();
        tag.setName("Finance");
        when(tagRepository.findByName("Finance")).thenReturn(Optional.of(tag));

        Optional<Tag> result = tagService.getTagByName("Finance");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Finance");
        verify(tagRepository).findByName("Finance");
    }

    @Test
    void getAllTags_shouldReturnAllTags() {
        Tag tag1 = new Tag();
        tag1.setName("Finance");
        Tag tag2 = new Tag();
        tag2.setName("Work");
        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));

        List<Tag> result = tagService.getAllTags();

        assertThat(result).hasSize(2)
                .extracting(Tag::getName)
                .containsExactly("Finance", "Work");
        verify(tagRepository).findAll();
    }

    @Test
    void deleteTag_shouldDeleteById() {
        UUID id = UUID.randomUUID();

        tagService.deleteTag(id);

        verify(tagRepository).deleteById(id);
    }
}
