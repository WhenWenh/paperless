package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.service.dto.TagDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TagMapperTest {

    private final TagMapper mapper = new TagMapper();

    @Test
    void mapToDto_shouldMapSingleEntity() {
        UUID id = UUID.randomUUID();
        Tag tag = Tag.builder().id(id).name("Finance").build();

        TagDto dto = mapper.mapToDto(tag);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo("Finance");
    }

    @Test
    void mapToDto_shouldSortListAlphabeticallyIgnoringCase() {
        List<Tag> tags = List.of(
                Tag.builder().name("work").build(),
                Tag.builder().name("Finance").build(),
                Tag.builder().name("archive").build()
        );

        List<TagDto> dtos = mapper.mapToDto(tags);

        assertThat(dtos).extracting(TagDto::getName)
                .containsExactly("archive", "Finance", "work");
    }

    @Test
    void mapToDto_shouldReturnEmptyListForNoEntities() {
        assertThat(mapper.mapToDto(List.<Tag>of())).isEmpty();
    }
}
