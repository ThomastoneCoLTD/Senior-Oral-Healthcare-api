package com.kaii.dentix.domain.contents.dao;

import com.kaii.dentix.domain.contents.domain.ContentsCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentsCardRepository extends JpaRepository<ContentsCard, Integer> {

    List<ContentsCard> findAllByContents_ContentsId(Long contentsId);
}
