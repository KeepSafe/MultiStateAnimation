#!/usr/bin/env python3
import argparse
import collections
import json
import pathlib
import re
import sys

class Spec(collections.OrderedDict):
    """An ordered defaultdict(dict)."""
    def __missing__(self, key):
        self[key] = ret = {}
        return ret


# A list of image filetypes used to filter out hidden files etc. from section
# directories
VALID_EXTENSITONS = {'.png', '.gif', '.bmp', '.jpg', '.jpeg'}

def answer_to_bool(answer):
    """Convert a string to a boolean."""
    a = answer.lower()
    if any(w.startswith(a) for w in ('yes', 'true', '1')):
        return True
    elif any(w.startswith(a) for w in ('no', 'false', '0')):
        return False

def get_answer(question, allow_empty=False):
    """Ask the user a question until they enter input."""
    ans = ''
    while not ans:
        ans = input(question.strip() + ' ')
        if allow_empty: 
            break
    return ans.strip()

def main():
    parser = argparse.ArgumentParser(
        description='Generate an animation specification file.'
        'e.x.\n`%(prog)s 01-start-image/  02-working/  03-end-transition/  04-end-image/`',
        epilog='''The arguments to this program are a series of directories.
        Each directory must contain the frames of a section of an overall
        animation. Frames can be any image file, but the names of the files
        must consist only of letters, numbers, and underscores. The program
        will ask a series of questions about each directory to generate the
        final specification. Each section must be given an ID, which does not
        have to match the directory name.''')
    parser.add_argument('directories', metavar='DIRS', nargs='+', type=pathlib.Path,
                       help='Directories of animation sequence. Each directory '
                       'should contain all of the frames for one section.')
    parser.add_argument('-d', '--frame-duration', type=int, default=33, metavar='INT',
                        help='Milliseconds per frame of the animation '
                        '(16 = 60fps, 33 = 30fps) [default: %(default)s]')
    parser.add_argument('-o', '--output', metavar='FILE', type=pathlib.Path,
                        help='If given, write output to %(metavar)s isntead of stdout.')

    args = parser.parse_args()

    spec = Spec()

    for path in args.directories:
        if not path.is_dir():
            print('Directory %s does not exist' % dirname)
            sys.exit(1)

        print('Working on directory %s...' % path)

        # The frames are the file names in the directory without path or extenstions
        frames = [f.stem for f in path.iterdir() if f.suffix in VALID_EXTENSITONS]
        for frame in frames:
            if re.search(r'^\w+$', frame) is None:
                print('ERROR: filename %s is not valid. Filenames must consist '
                      'only of letters, numbers and underscores' % frame)
                sys.exit(1)

        if len(frames) > 1:
            if answer_to_bool(get_answer('Are these frames a transition [y/n]?')):
                from_id = get_answer(
                    'What is the ID that these frames are a transition FROM '
                    '(leave empty for initial transition)?', allow_empty=True)
                section_id = get_answer('What is the ID that these frames are a transition TO?')
                # Create the section if it doesn't exist yet.
                spec[section_id].setdefault('transitions_from', {})[from_id] = {
                    'frame_duration': args.frame_duration,
                    'frames': frames
                }
                print()
                continue
            
        section_id = get_answer('What is the name of this section?')

        # Single-frame sections are automatically oneshot with default duration.
        if len(frames) == 1:
            oneshot = True
        else:
            spec[section_id]['frame_duration'] = args.frame_duration
            oneshot = answer_to_bool(get_answer('Is this section a oneshot [y/n]?'))

        spec[section_id]['oneshot'] = oneshot
        spec[section_id]['frames'] = frames

        print()

    if args.output:
        with open(args.output, 'w') as f:
            json.dump(spec, f, indent=4)
    else:
        print(json.dumps(spec, indent=4))


if __name__ == '__main__':
    main()
