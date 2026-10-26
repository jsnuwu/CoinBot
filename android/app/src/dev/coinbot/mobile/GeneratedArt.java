package dev.coinbot.mobile;

import java.util.*;

/** Evidence from the actual accepted pixels, not a reconstructed guess after a retry. */
final class GeneratedArt {
    final StoryPlan plan;final ImageFingerprint fingerprint;final int attempt,historySize;
    GeneratedArt(StoryPlan plan,ImageFingerprint fingerprint,int attempt,int historySize){this.plan=plan;this.fingerprint=fingerprint;this.attempt=attempt;this.historySize=historySize;}
    Map<String,Object> metadata(){
        Map<String,Object> fields=new LinkedHashMap<>();
        fields.put("image_generator",StoryPlan.GENERATOR);fields.put("image_scene","stage_"+plan.environment+"_actor_"+plan.actor+"_prop_"+plan.prop);fields.put("image_topic_motif",plan.scene.name);fields.put("image_source_asset","meme-stages-v4.png");fields.put("image_actor_asset","meme-actors-v4.png");
        fields.put("image_composition",plan.structure);fields.put("image_seed",Long.toHexString(plan.seed));fields.put("image_attempt",attempt+1);
        fields.put("image_actor",plan.actor);fields.put("image_companion",plan.companion);fields.put("image_layout",plan.layout);fields.put("image_environment",plan.environment);fields.put("image_prop",plan.prop);
        fields.put("image_pixel_sha256",fingerprint.pixels);fields.put("image_structure_hash",Long.toHexString(fingerprint.edges));
        fields.put("image_uniqueness_check","pixels_and_structure_v1");fields.put("image_history_entries",historySize);
        return fields;
    }
}
