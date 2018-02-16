if github.branch_for_base == "master"
  if !git.modified_files.include? "config/*"
    warn 'Did you remember to generate documentation via dokku? (Hint: `./gradlew dokka`)'
  end
end
