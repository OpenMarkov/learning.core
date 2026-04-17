/*
 * Copyright (c) CISIAD, UNED, Spain, 2026. Licensed under the GPLv3 licence
 */

package org.openmarkov.learning.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmarkov.core.action.base.linkEdits.AddLinkEdit;
import org.openmarkov.core.model.network.NodeType;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LearningEditProposal}: verifies the {@code equals}/{@code hashCode} contract,
 * natural ordering via {@code compareTo}, sorting behavior, and {@code toString} output.
 *
 * @author Manuel Arias
 */
class LearningEditProposalTest {

    private ProbNet probNet;
    private Variable vA;
    private Variable vB;

    @BeforeEach
    void setUp() {
        probNet = new ProbNet();
        vA = new Variable("A", "0", "1");
        vB = new Variable("B", "0", "1");
        probNet.addNode(vA, NodeType.CHANCE);
        probNet.addNode(vB, NodeType.CHANCE);
    }

    @Test
    void gettersReturnConstructorArguments() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var motivation = new ScoreEditMotivation(1.5);
        var proposal = new LearningEditProposal(edit, motivation);

        assertThat(proposal.getEdit()).isSameAs(edit);
        assertThat(proposal.getMotivation()).isSameAs(motivation);
    }

    @Test
    void sameEditAndEqualMotivationAreEqual() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);

        var proposal1 = new LearningEditProposal(edit, new ScoreEditMotivation(1.5));
        var proposal2 = new LearningEditProposal(edit, new ScoreEditMotivation(1.5));

        assertThat(proposal1).isEqualTo(proposal2);
        assertThat(proposal1.hashCode()).isEqualTo(proposal2.hashCode());
    }

    @Test
    void differentEditsAreNotEqual() {
        var editAB = new AddLinkEdit(probNet, vA, vB, true);
        var editBA = new AddLinkEdit(probNet, vB, vA, true);
        var motivation = new ScoreEditMotivation(1.0);

        var proposal1 = new LearningEditProposal(editAB, motivation);
        var proposal2 = new LearningEditProposal(editBA, motivation);

        assertThat(proposal1).isNotEqualTo(proposal2);
    }

    @Test
    void notEqualToNull() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var proposal = new LearningEditProposal(edit, new ScoreEditMotivation(1.0));

        assertThat(proposal).isNotEqualTo(null);
    }

    @Test
    void notEqualToDifferentType() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var proposal = new LearningEditProposal(edit, new ScoreEditMotivation(1.0));

        assertThat(proposal).isNotEqualTo("a string");
    }

    @Test
    void equalToSelf() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var proposal = new LearningEditProposal(edit, new ScoreEditMotivation(1.0));

        assertThat(proposal).isEqualTo(proposal);
    }

    @Test
    void compareToOrdersByMotivationScore() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var low = new LearningEditProposal(edit, new ScoreEditMotivation(1.0));
        var high = new LearningEditProposal(edit, new ScoreEditMotivation(5.0));

        assertThat(low.compareTo(high)).isNegative();
        assertThat(high.compareTo(low)).isPositive();
    }

    @Test
    void compareToAllowsSorting() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var p3 = new LearningEditProposal(edit, new ScoreEditMotivation(3.0));
        var p1 = new LearningEditProposal(edit, new ScoreEditMotivation(1.0));
        var p2 = new LearningEditProposal(edit, new ScoreEditMotivation(2.0));

        List<LearningEditProposal> proposals = new ArrayList<>(List.of(p3, p1, p2));
        Collections.sort(proposals);

        assertThat(proposals).containsExactly(p1, p2, p3);
    }

    @Test
    void factoryMethodCreatesScoreBasedProposal() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var proposal = LearningEditProposal.scored(edit, 3.14);

        assertThat(proposal.getEdit()).isSameAs(edit);
        assertThat(proposal.getMotivation()).isInstanceOf(ScoreEditMotivation.class);
        assertThat(((ScoreEditMotivation) proposal.getMotivation()).getScore()).isEqualTo(3.14);
    }

    @Test
    void toStringContainsEditAndMotivation() {
        var edit = new AddLinkEdit(probNet, vA, vB, true);
        var motivation = new ScoreEditMotivation(2.5);
        var proposal = new LearningEditProposal(edit, motivation);

        String result = proposal.toString();

        assertThat(result).contains(motivation.toString());
    }
}
