package com.pingAssignment.scalablefileprocessor.repository;

import com.pingAssignment.scalablefileprocessor.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


public class UserRepositoryTest {
    @Mock
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindById() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User1");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        Optional<User> found = userRepository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("User1");
    }

    @Test
    public void testFindById_ReturnsEmptyOptional() {
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        Optional<User> found = userRepository.findById(2L);

        assertThat(found).isNotPresent();
    }

    @Test
    public void testFindById_ThrowsNoSuchElementExceptionOnGet() {
        given(userRepository.findById(3L)).willReturn(Optional.empty());

        Optional<User> found = userRepository.findById(3L);

        assertThrows(NoSuchElementException.class, found::get);
    }

    @Test
    public void testSave_ThrowsRuntimeException() {
        User user = new User();
        user.setFirstName("Error");
        user.setLastName("User");

        doThrow(new RuntimeException("DB error")).when(userRepository).save(user);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userRepository.save(user);
        });

        assertThat(thrown.getMessage()).isEqualTo("DB error");

        verify(userRepository).save(user);
    }

    @Test
    public void testSave_NullUser_ThrowsIllegalArgumentException() {
        doThrow(new IllegalArgumentException("Cannot save null user"))
                .when(userRepository).save(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userRepository.save(null);
        });

        assertThat(thrown.getMessage()).isEqualTo("Cannot save null user");

        verify(userRepository).save(null);
    }

}
