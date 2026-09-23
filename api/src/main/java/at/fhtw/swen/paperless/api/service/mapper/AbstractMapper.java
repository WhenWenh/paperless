package at.fhtw.swen.paperless.api.service.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractMapper<E, D> {
    public abstract D toDto(E entity);

    public abstract E toEntity(D dto);

    public final List<D> toDto(Collection<E> entities) {
        List<D> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return sort(dtos);
    }

    protected List<D> sort(List<D> dtos) {
        return dtos;
    }
}
