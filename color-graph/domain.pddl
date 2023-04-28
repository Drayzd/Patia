(define (domain graph-coloring)
  (:requirements :typing)
  (:types node color)
  (:predicates
    (colored ?n ?c- node)
    ;(conflict ?n1 ?n2 - node)
    (connected ?n1 ?n2 - node)
  )
  
  (:action color-node
    :parameters (?n - node ?c - color)
    :precondition (and (not (colored ?n)) (forall (?conn - node) (implies (connected ?n ?conn) and (not (colored ?conn ?c)))))
    :effect (colored ?n ?c)
    )
)
  
