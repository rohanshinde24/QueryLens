package com.querylens.optimizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QueryRewriteServiceTest {

    @Mock
    private QueryRewriter rewriter1;

    @Mock
    private QueryRewriter rewriter2;

    private QueryRewriteService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new QueryRewriteService(List.of(rewriter1, rewriter2));
    }

    @Test
    void rewrite_appliesFirstMatchingRewriter() {
        String sql = "SELECT * FROM users";
        List<String> plan = List.of();
        String expectedRewritten = "SELECT id, name FROM users";

        when(rewriter1.canRewrite(sql, plan)).thenReturn(true);
        when(rewriter1.rewrite(sql, plan)).thenReturn(expectedRewritten);

        Optional<String> result = service.rewrite(sql, plan);

        assertThat(result).isPresent().contains(expectedRewritten);
        verify(rewriter1).canRewrite(sql, plan);
        verify(rewriter1).rewrite(sql, plan);
        // Second rewriter should not be checked
        verify(rewriter2, never()).canRewrite(anyString(), anyList());
    }

    @Test
    void rewrite_triesSecondRewriterIfFirstDoesNotMatch() {
        String sql = "SELECT * FROM users";
        List<String> plan = List.of();
        String expectedRewritten = "WITH cte AS (...) SELECT ...";

        when(rewriter1.canRewrite(sql, plan)).thenReturn(false);
        when(rewriter2.canRewrite(sql, plan)).thenReturn(true);
        when(rewriter2.rewrite(sql, plan)).thenReturn(expectedRewritten);

        Optional<String> result = service.rewrite(sql, plan);

        assertThat(result).isPresent().contains(expectedRewritten);
        verify(rewriter1).canRewrite(sql, plan);
        verify(rewriter1, never()).rewrite(anyString(), anyList());
        verify(rewriter2).canRewrite(sql, plan);
        verify(rewriter2).rewrite(sql, plan);
    }

    @Test
    void rewrite_returnsEmptyIfNoRewriterMatches() {
        String sql = "SELECT id FROM users";
        List<String> plan = List.of();

        when(rewriter1.canRewrite(sql, plan)).thenReturn(false);
        when(rewriter2.canRewrite(sql, plan)).thenReturn(false);

        Optional<String> result = service.rewrite(sql, plan);

        assertThat(result).isEmpty();
        verify(rewriter1).canRewrite(sql, plan);
        verify(rewriter2).canRewrite(sql, plan);
        verify(rewriter1, never()).rewrite(anyString(), anyList());
        verify(rewriter2, never()).rewrite(anyString(), anyList());
    }

    @Test
    void rewrite_handlesEmptyRewriterList() {
        QueryRewriteService emptyService = new QueryRewriteService(List.of());
        
        Optional<String> result = emptyService.rewrite("SELECT * FROM users", List.of());

        assertThat(result).isEmpty();
    }
}

