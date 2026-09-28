package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.AiDataSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiDataSourceRepository extends JpaRepository<AiDataSource, Long> {

    List<AiDataSource> findAllByOrderByUpdatedAtDescIdDesc();
}
