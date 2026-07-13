package com.parazit.panel.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.content.faq.FaqItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class FaqPropertiesTest {

    @Test
    void sortsEnabledItemsDeterministically() {
        FaqProperties properties = new FaqProperties(true, 6, List.of(
                new FaqItem("b", true, 20, "B?", "B", List.of(), "1"),
                new FaqItem("a", true, 10, "A?", "A", List.of(), "1")
        ));

        assertThat(properties.enabledItems()).extracting(FaqItem::id).containsExactly("a", "b");
    }

    @Test
    void rejectsDuplicateIdsAndInvalidPageSize() {
        List<FaqItem> items = List.of(
                new FaqItem("same", true, 10, "A?", "A", List.of(), "1"),
                new FaqItem("same", true, 20, "B?", "B", List.of(), "1")
        );

        assertThatThrownBy(() -> new FaqProperties(true, 6, items))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FaqProperties(true, 20, List.of(items.getFirst())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
