package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.service.dto.TagDto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class TagMapper extends AbstractMapper<Tag, TagDto> {

    @Override
    public TagDto toDto(Tag entity) {
        return TagDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    @Override
    public Tag toEntity(TagDto dto) {
        return Tag.builder()
                .name(dto.getName())
                .build();
    }

    @Override
    protected List<TagDto> sort(List<TagDto> tags) {
        tags.sort(Comparator.comparing(
                TagDto::getName,
                String.CASE_INSENSITIVE_ORDER
        ));
        return tags;
    }
}