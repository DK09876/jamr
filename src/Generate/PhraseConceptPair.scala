package edu.cmu.lti.nlp.amr.Generate
import edu.cmu.lti.nlp.amr._

import scala.util.matching.Regex
import scala.collection.mutable.{Map, Set, ArrayBuffer}

// Example: Phrase("offer", "offer-01", "NN", "NN")
// Example: offer ||| offer-01 ||| NN ||| NN

// I know this class is duplicated in ConceptInvoke.PhraseConceptPair.
// But it's ok there are differences between the two because the application is different (and they may diverge futher)

case class PhraseConceptPair(words: String, graphFrag: String, fullPos: String, headPos: String) {
    // graphFrag shouldn't contain variable names.  Example:
    //   (date-entity :day 5 :month 1 :year 2002)
    // This is necessary so that phraseConcept pairs are identified as the same regardless
    // of ambiguities in variable names.  Remember, the graphFrag is a tree so re-entrancies
    // are not necessary.
    def mkRule : String = {
       return Rule.graphToCFG(Graph.parse(graphFrag).root)+") ||| "+words
    }
    /*def lhs : List[(String, String)] = {  // TODO: if compiles, remove
        return Rule.mkLhsList(Graph.parse(graphFrag))
    }*/
    //def realization : String = words      // TODO: if compiles, remove
    override def toString : String = {
        return graphFrag + " ||| " + words + " ||| " + fullPos + " ||| " + headPos
    }
    def amrInstance : Node = {
        return Graph.parse(graphFrag).root
    }
    lazy val concept : String = amrInstance.concept
    def matches(amr: Node, ignoreSense: Boolean) : Boolean = {    // returns if the concept fragment could match the amr (but the amr may have extra children)
        return PhraseConceptPair.matches(amrInstance, amr, ignoreSense)
    }
    def changeConceptTo(node: Node) : PhraseConceptPair = {
        logger(0,"changeConceptTo "+node.concept)
        logger(0,"this = "+this)
        if (!graphFrag.startsWith("(")) {   // only change the concept when it's a singe concept with no children
            logger(0,"attempting to change")
            logger(0,"returning: "+PhraseConceptPair(words, node.concept, fullPos, headPos))
            PhraseConceptPair(words, node.concept, fullPos, headPos)
        } else {
            logger(0,"not attempting to change")
            logger(0,"returning: "+this)
            this
        }
    }
}

object PhraseConceptPair {
    def fromSpan(span: Span, pos: Array[String]) : PhraseConceptPair = {
        // Assumes head final for calculating headPos (ok heuristic for English)
        //logger(1, "span: " + span.format + " " + span.words)
        //logger(1, "pos: " + pos.toList.toString)
        logger(0, "PhraseConceptPair lowercase = "+lowercase.toString)
        return PhraseConceptPair(if (lowercase) { span.words.toLowerCase } else { span.words },
                                 span.amr.prettyString(0, false, Set.empty[String]), /*no variable names*/
                                 pos.slice(span.start, span.end).mkString(" "),
                                 if(span.start < span.end) { pos(span.end-1) } else { "" })
    }

    def apply(string: String) : PhraseConceptPair = {    // TODO: could also do unapply, so you can do val Rule(...) = string
        val regex = """(.*) \|\|\| (.*) \|\|\| ([^\|]*) \|\|\| ([^|]*)""".r
        val regex(graphFrag, words, fullPos, headPos) = string
        return PhraseConceptPair(words, graphFrag, fullPos, headPos)
    }

    def matches(rule: String, amr: Node, ignoreSense: Boolean) : Boolean = {
        return matches(Graph.parse(rule).root, amr, ignoreSense)
    }

    def matches(rule: Node, amr: Node, ignoreSense: Boolean) : Boolean = {
        // doesn't care if there are extra children in amr
        // we allow concepts in the rule to be "<X>", which are variables that match anything
        if (!ignoreSense && rule.concept != amr.concept && rule.concept != "<X>") {  // concepts don't match
            return false
        } else if (ignoreSense && dropSense(rule.concept) != dropSense(amr.concept)) {
            return false
        }
        var ruleChildren : List[(String, Node)] = rule.children
        var amrChildren : Array[((String, Node), Boolean)] = amr.children.map(x => (x, false)).toArray
        var matching = true
        for ((ruleRelation, ruleChild) <- ruleChildren) {
            var foundMatch = false
            for { (((amrRelation, amrChild), used), i) <- amrChildren.zipWithIndex
                  if (ruleRelation == amrRelation && !foundMatch && !used)
                    } {
                if (matches(ruleChild, amrChild, ignoreSense)) {
                    amrChildren(i) = ((amrRelation, amrChild), true)    // mark this child as matched (used)
                    foundMatch = true    // break loop and indicate we have found a match
                }
            }
            if (!foundMatch) {
                matching = false    // there's a child that doesn't match
            }
        }
        return matching
    }

    def matchesExactly(rule: Node, amr: Node) : Boolean = {
        // doesn't allow extra children in amr
        // we allow concepts in the rule to be "<X>", which are variables that match anything
        if (rule.concept != amr.concept && rule.concept != "<X>") {  // concepts don't match
            return false
        }
        var ruleChildren : List[(String, Node)] = rule.children
        var amrChildren : Array[((String, Node), Boolean)] = amr.children.map(x => (x, false)).toArray
        var matching = true
        for ((ruleRelation, ruleChild) <- ruleChildren) {
            var foundMatch = false
            for { (((amrRelation, amrChild), used), i) <- amrChildren.zipWithIndex
                  if (ruleRelation == amrRelation && !foundMatch && !used)
                    } {
                if (matchesExactly(ruleChild, amrChild)) {
                    amrChildren(i) = ((amrRelation, amrChild), true)    // mark this child as matched (used)
                    foundMatch = true    // break loop and indicate we have found a match
                }
            }
            if (!foundMatch) {
                matching = false    // there's a child that doesn't match
            }
        }
        return matching && (amrChildren :\ true)((x, result) => x._2 && result)
    }
}
