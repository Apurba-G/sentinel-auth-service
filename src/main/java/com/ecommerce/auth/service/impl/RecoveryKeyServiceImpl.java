package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserBackupCode;
import com.ecommerce.auth.repository.UserBackupCodeRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.RecoveryKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryKeyServiceImpl implements RecoveryKeyService {

    private final UserRepository userRepository;
    private final UserBackupCodeRepository backupCodeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String[] BIP39_WORDS = {
            "abandon", "ability", "absent", "absorb", "abstract", "academy", "access", "account",
            "accuse", "achieve", "acid", "acoustic", "acquire", "action", "active", "actor",
            "adapt", "address", "advance", "advice", "aerobic", "affair", "afford", "afraid",
            "agent", "agree", "ahead", "aim", "airport", "aisle", "alarm", "album",
            "alcohol", "alert", "alien", "all", "alley", "allow", "almost", "alone",
            "alpha", "already", "also", "alter", "always", "amateur", "amazing", "among",
            "amount", "amused", "analyst", "anchor", "ancient", "anger", "angle", "angry",
            "animal", "ankle", "announce", "annual", "another", "answer", "antenna", "antique",
            "anxiety", "any", "apart", "apology", "appear", "apple", "approve", "april",
            "arch", "arctic", "area", "arena", "argue", "arm", "armed", "armor",
            "army", "around", "arrange", "arrest", "arrive", "arrow", "art", "artefact",
            "artist", "artwork", "ask", "aspect", "assault", "asset", "assist", "assume",
            "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract", "auction",
            "audit", "august", "aunt", "author", "auto", "autumn", "average", "avocado",
            "avoid", "awake", "aware", "away", "awesome", "awful", "awkward", "axis",
            "baby", "bachelor", "bacon", "badge", "bag", "balance", "balcony", "ball"
    };

    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateMnemonicPhrase() {
        StringBuilder phrase = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = random.nextInt(BIP39_WORDS.length);
            phrase.append(BIP39_WORDS[index]);
            if (i < 11) {
                phrase.append(" ");
            }
        }
        return phrase.toString();
    }

    @Override
    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            StringBuilder part1 = new StringBuilder(4);
            StringBuilder part2 = new StringBuilder(4);
            for (int j = 0; j < 4; j++) {
                part1.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
                part2.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            codes.add(part1 + "-" + part2);
        }
        return codes;
    }

    @Override
    @Transactional
    public void saveRecoveryPhrase(User user, String plaintextPhrase) {
        String normalized = plaintextPhrase.trim().toLowerCase().replaceAll("\\s+", " ");
        user.setRecoveryPhraseHash(passwordEncoder.encode(normalized));
        userRepository.save(user);
        log.info("Recovery phrase hash securely registered for user {}", user.getId());
    }

    @Override
    @Transactional
    public void saveBackupCodes(User user, List<String> plaintextCodes) {
        for (String code : plaintextCodes) {
            String normalized = code.trim().toUpperCase().replace("-", "");
            UserBackupCode backupCode = UserBackupCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(normalized))
                    .used(false)
                    .build();
            backupCodeRepository.save(backupCode);
        }
        log.info("Registered {} backup recovery codes for user {}", plaintextCodes.size(), user.getId());
    }

    @Override
    public boolean verifyRecoveryPhrase(User user, String providedPhrase) {
        if (user.getRecoveryPhraseHash() == null || providedPhrase == null) {
            return false;
        }
        String normalized = providedPhrase.trim().toLowerCase().replaceAll("\\s+", " ");
        return passwordEncoder.matches(normalized, user.getRecoveryPhraseHash());
    }

    @Override
    @Transactional
    public boolean verifyAndConsumeBackupCode(User user, String providedCode) {
        if (providedCode == null || user == null) {
            return false;
        }
        String normalized = providedCode.trim().toUpperCase().replace("-", "");
        List<UserBackupCode> availableCodes = backupCodeRepository.findByUserIdAndUsedFalse(user.getId());

        for (UserBackupCode codeEntity : availableCodes) {
            if (passwordEncoder.matches(normalized, codeEntity.getCodeHash())) {
                codeEntity.setUsed(true);
                codeEntity.setUsedAt(OffsetDateTime.now());
                backupCodeRepository.save(codeEntity);
                log.info("Consumed single-use backup recovery code for user {}", user.getId());
                return true;
            }
        }
        return false;
    }
}
