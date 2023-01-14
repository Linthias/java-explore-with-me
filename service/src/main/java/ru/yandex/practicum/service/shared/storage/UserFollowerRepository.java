package ru.yandex.practicum.service.shared.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.service.shared.model.UserFollower;

import java.util.Optional;


@Repository
public interface UserFollowerRepository extends JpaRepository<UserFollower, Long> {
    @Query(value = "select id from users_followers " +
            "where user_id = :userId and follower_id = :followerId", nativeQuery = true)
    Optional<Long> findUserFollowerId(@Param("userId") long userId, @Param("followerId") long followerId);

    boolean existsByUserIdAndFollowerId(long userId, long followerId);
}
