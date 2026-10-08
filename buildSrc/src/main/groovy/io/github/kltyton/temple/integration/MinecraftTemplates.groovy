package io.github.kltyton.temple.integration

import groovy.json.JsonOutput
import io.github.kltyton.temple.workspace.ProjectFiles
import io.github.kltyton.temple.workspace.TargetDefinition

final class MinecraftTemplates {
    static void export(File root, File output) {
        List<TargetDefinition> targets = ProjectFiles.targets(root)
        Properties identity = ProjectFiles.read(new File(root, 'gradle.properties'))
        String original = identity.getProperty('mod_java_package', identity.getProperty('mod_group_id'))
        File assets = new File(output, 'assets')
        assets.mkdirs()
        Set<String> blueprints = ['fabric-1.20.1', 'fabric-26.1.2', 'forge-1.20.1',
                                   'neoforge-1.21.1', 'neoforge-26.1.2'] as Set
        List<Map<String, Object>> files = []
        Set<String> roots = ['buildSrc', 'common', 'versions', 'loaders', 'gradle', 'targets', 'integrations', 'docs', '.github'] as Set
        Set<String> rootFiles = ['build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
                                  'LICENSE', 'README.md', 'README.en.md', '.gitignore', '.gitattributes'] as Set
        ProjectFiles.distributableFiles(root).each { File source ->
            String relative = root.toPath().relativize(source.toPath()).toString().replace('\\', '/')
            if (relative == 'gradle.properties' || relative.startsWith('integrations/minecraft-development/') ||
                    relative.startsWith('integrations/idea-plugin/') ||
                    !(relative.tokenize('/')[0] in roots || relative in rootFiles) || source.name.endsWith('.jar')) { return }
            String destination = relative
            String content = source.getText('UTF-8')
            if (source.name.endsWith('.java') && relative.tokenize('/')[0] in ['common', 'versions', 'loaders', 'targets']) {
                destination = relative.replace(original.replace('.', '/'), '${BUILD_COORDS.groupId.replace(".", "/")}')
                content = literalParts(content, original, '${BUILD_COORDS.groupId}')
            } else { content = literal(content) }
            File asset = new File(assets, relative + '.ft')
            asset.parentFile.mkdirs()
            asset.setText(content, 'UTF-8')
            Map<String, Object> descriptor = [:]
            descriptor.put('template', 'assets/' + relative + '.ft')
            descriptor.put('destination', destination)
            descriptor.put('reformat', false)
            if (relative.startsWith('targets/')) {
                String id = relative.tokenize('/')[1]
                descriptor.condition = "\$TARGET_PLAN.contains('\"blueprint\":\"${id}\"')"
                if (id in blueprints) {
                    files.add([template: descriptor.template,
                               destination: relative.replace('targets/', 'gradle/target-blueprints/'), reformat: false])
                }
            }
            files.add(descriptor)
        }
        String properties = '#[[org.gradle.jvmargs=-Xmx3G -Dfile.encoding=UTF-8\norg.gradle.daemon=false\n]]#' +
                'mod_id=${MOD_ID}\nmod_name=${MOD_NAME}\nmod_group_id=${BUILD_COORDS.groupId}\n' +
                'mod_authors=${AUTHORS}\nmod_version=${BUILD_COORDS.version}\nmod_license=${LICENSE}\n' +
                'mod_description=${DESCRIPTION}\ntemple_idea_project=true\n'
        new File(assets, 'project.properties.ft').setText(properties, 'UTF-8')
        files.add([template: 'assets/project.properties.ft', destination: 'gradle.properties', reformat: false])
        File wrapper = new File(root, 'gradle/wrapper/gradle-wrapper.jar')
        new File(assets, 'wrapper.base64.ft').setText(wrapper.bytes.encodeBase64().toString(), 'UTF-8')
        files.add([template: 'assets/wrapper.base64.ft', destination: 'gradle/wrapper/kltyton-wrapper.base64', reformat: false])
        targets.each { target ->
            String condition = "\$TARGET_PLAN.contains('\"blueprint\":\"${target.id}\"')"
            files.add([template: 'assets/wrapper.base64.ft',
                       destination: "targets/${target.id}/gradle/wrapper/kltyton-wrapper.base64",
                       condition: condition, reformat: false])
        }
        def descriptor = [version: 3, label: 'KltytonTemple', group: 'mod',
            properties: [
                [name:'BUILD_COORDS', type:'build_system_coordinates', order:10],
                [name:'MOD_ID', type:'string', label:'Mod ID', default:'my_mod', validator:'[a-z][a-z0-9_]{1,63}',
                 derives:[parents:['PROJECT_NAME'], method:'replace',
                          parameters:[regex:'[^a-z0-9_]+', replacement:'_', lowercase:true, maxLength:64]]],
                [name:'MOD_NAME', type:'string', label:'Mod 名称', inheritFrom:'PROJECT_NAME', default:'My Mod'],
                [name:'TARGET_PLAN', type:'kltyton_targets', label:'Minecraft / Loader'],
                [name:'AUTHORS', type:'string', label:'作者', default:'kltyton'],
                [name:'LICENSE', type:'string', label:'许可证', default:'MIT'],
                [name:'DESCRIPTION', type:'string', label:'描述', default:'A Minecraft mod.'],
                [name:'JDK', type:'jdk', default:21, order:20]
            ],
            files: files,
            finalizers: [[type:'kltyton_targets'], [type:'import_gradle_project'],
                         [type:'run_gradle_tasks', tasks:['setupProject']]]
        ]
        new File(output, '.mcdev.template.json').setText(JsonOutput.prettyPrint(JsonOutput.toJson(descriptor)), 'UTF-8')
    }

    private static String literal(String text) {
        '#set($END_BLOCK = \'\u005d\u005d#\')\n' + literalBody(text)
    }

    private static String literalBody(String text) {
        text.split(java.util.regex.Pattern.quote(']]#'), -1)
                .collect { '#[[' + it + ']]#' }.join('\$END_BLOCK')
    }

    private static String literalParts(String text, String original, String expression) {
        '#set($END_BLOCK = \'\u005d\u005d#\')\n' +
                text.split(java.util.regex.Pattern.quote(original), -1).collect { literalBody(it) }.join(expression)
    }
}
